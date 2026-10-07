package com.group01.assessment.infrastructure.persistence.adapter;

import com.group01.assessment.application.port.GateEssayJobStore;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** PostgreSQL persistence for the bounded, asynchronous essay grading queue. */
@Repository
public class JdbcGateEssayJobStore implements GateEssayJobStore {
    private static final int MAX_CLAIM_BATCH = 50;
    private final NamedParameterJdbcTemplate jdbc;

    public JdbcGateEssayJobStore(NamedParameterJdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    public Map<UUID, UUID> submittedEssays(Collection<UUID> attemptItemIds) {
        if (attemptItemIds == null || attemptItemIds.isEmpty()) return Map.of();
        String sql = """
                SELECT DISTINCT ON (attempt_item_id) attempt_item_id, id
                FROM learner_submissions
                WHERE attempt_item_id IN (:itemIds) AND status = 'SUBMITTED' AND skill = 'WRITING'
                  AND text_payload IS NOT NULL
                ORDER BY attempt_item_id, submitted_at DESC, id DESC
                """;
        List<Map.Entry<UUID, UUID>> rows = jdbc.query(sql, new MapSqlParameterSource("itemIds", attemptItemIds),
                (rs, rowNum) -> Map.entry(rs.getObject("attempt_item_id", UUID.class),
                        rs.getObject("id", UUID.class)));
        Map<UUID, UUID> found = new LinkedHashMap<>();
        rows.forEach(row -> found.put(row.getKey(), row.getValue()));
        return Map.copyOf(found);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void enqueueAi(Map<UUID, UUID> attemptItemToSubmission, UUID userId) {
        if (attemptItemToSubmission == null || attemptItemToSubmission.isEmpty()) return;
        String sql = """
                INSERT INTO grading_jobs (id, submission_id, user_id, skill, grading_mode, status,
                                          point_cost_snapshot, idempotency_key, created_at)
                SELECT gen_random_uuid(), ls.id, ls.user_id, ls.skill, 'AI', 'QUEUED', 0,
                       'gate-essay:' || ls.attempt_item_id || ':ai', CURRENT_TIMESTAMP
                FROM learner_submissions ls
                WHERE ls.id = :submissionId AND ls.attempt_item_id = :attemptItemId
                  AND ls.user_id = :userId AND ls.status = 'SUBMITTED' AND ls.skill = 'WRITING'
                ON CONFLICT (idempotency_key) DO NOTHING
                """;
        SqlParameterSource[] batch = attemptItemToSubmission.entrySet().stream()
                .map(entry -> new MapSqlParameterSource().addValue("submissionId", entry.getValue())
                        .addValue("attemptItemId", entry.getKey()).addValue("userId", userId))
                .toArray(SqlParameterSource[]::new);
        jdbc.batchUpdate(sql, batch);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void enqueueHuman(UUID submissionId, UUID userId, String idempotencyKey) {
        String sql = """
                INSERT INTO grading_jobs (id, submission_id, user_id, skill, grading_mode, status,
                                          point_cost_snapshot, idempotency_key, created_at)
                SELECT gen_random_uuid(), id, user_id, skill, 'HUMAN', 'QUEUED', NULL, :key, CURRENT_TIMESTAMP
                FROM learner_submissions
                WHERE id = :submissionId AND user_id = :userId AND status = 'SUBMITTED'
                ON CONFLICT (idempotency_key) DO NOTHING
                RETURNING id
                """;
        List<UUID> inserted = jdbc.query(sql, new MapSqlParameterSource().addValue("submissionId", submissionId)
                        .addValue("userId", userId).addValue("key", idempotencyKey),
                (rs, rowNum) -> rs.getObject("id", UUID.class));
        if (!inserted.isEmpty()) {
            jdbc.update("INSERT INTO human_reviews (id, grading_job_id, status) VALUES (gen_random_uuid(), :jobId, 'QUEUED')",
                    Map.of("jobId", inserted.getFirst()));
        }
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public int requeueStuck(Instant startedBefore) {
        return jdbc.update("""
                UPDATE grading_jobs
                SET status = 'QUEUED', processing_started_at = NULL
                WHERE grading_mode = 'AI' AND status = 'PROCESSING' AND processing_started_at < :startedBefore
                """, Map.of("startedBefore", Timestamp.from(startedBefore)));
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public List<ClaimedJob> claim(int limit, Instant now) {
        int boundedLimit = Math.max(0, Math.min(limit, MAX_CLAIM_BATCH));
        if (boundedLimit == 0) return List.of();
        String sql = """
                WITH candidates AS (
                    SELECT gj.id
                    FROM grading_jobs gj
                    JOIN learner_submissions ls ON ls.id = gj.submission_id
                    JOIN attempt_items ai ON ai.id = ls.attempt_item_id
                    JOIN attempt_sections ase ON ase.id = ai.attempt_section_id
                    JOIN assessment_attempts aa ON aa.id = ase.attempt_id
                    WHERE gj.grading_mode = 'AI' AND gj.status = 'QUEUED'
                      AND aa.attempt_type IN ('TOPIC_GATE', 'COURSE_GATE')
                    ORDER BY gj.created_at, gj.id
                    LIMIT :limit
                    FOR UPDATE OF gj SKIP LOCKED
                )
                UPDATE grading_jobs gj
                SET status = 'PROCESSING', processing_started_at = :now
                FROM candidates c, learner_submissions ls, attempt_items ai,
                     attempt_sections ase, assessment_attempts aa
                WHERE gj.id = c.id AND ls.id = gj.submission_id AND ai.id = ls.attempt_item_id
                  AND ase.id = ai.attempt_section_id AND aa.id = ase.attempt_id
                RETURNING gj.id AS job_id, gj.user_id, aa.id AS attempt_id, ai.id AS attempt_item_id,
                          ls.id AS submission_id, ls.text_payload AS essay,
                          ai.question_snapshot::text AS question_snapshot,
                          ai.answer_snapshot::text AS answer_snapshot
                """;
        return jdbc.query(sql, new MapSqlParameterSource().addValue("limit", boundedLimit)
                        .addValue("now", Timestamp.from(now)),
                JdbcGateEssayJobStore::claimedJob);
    }

    private static ClaimedJob claimedJob(ResultSet rs, int rowNum) throws SQLException {
        return new ClaimedJob(rs.getObject("job_id", UUID.class), rs.getObject("user_id", UUID.class),
                rs.getObject("attempt_id", UUID.class), rs.getObject("attempt_item_id", UUID.class),
                rs.getObject("submission_id", UUID.class), rs.getString("essay"),
                rs.getString("question_snapshot"), rs.getString("answer_snapshot"));
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void complete(UUID jobId, BigDecimal band, Instant now) {
        jdbc.update("""
                UPDATE grading_jobs SET status = 'COMPLETED', llm_band = :band,
                    completed_at = :now, processing_started_at = NULL
                WHERE id = :id AND grading_mode = 'AI' AND status = 'PROCESSING'
                """, new MapSqlParameterSource().addValue("id", jobId).addValue("band", band)
                .addValue("now", Timestamp.from(now)));
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void fail(UUID jobId, Instant now) {
        jdbc.update("""
                UPDATE grading_jobs SET status = 'FAILED', completed_at = :now, processing_started_at = NULL
                WHERE id = :id AND grading_mode = 'AI' AND status = 'PROCESSING'
                """, new MapSqlParameterSource().addValue("id", jobId).addValue("now", Timestamp.from(now)));
    }

    @Override
    public List<JobState> aiJobs(UUID attemptId) {
        return jdbc.query("""
                SELECT ai.id AS attempt_item_id, gj.status, gj.llm_band
                FROM grading_jobs gj
                JOIN learner_submissions ls ON ls.id = gj.submission_id
                JOIN attempt_items ai ON ai.id = ls.attempt_item_id
                JOIN attempt_sections ase ON ase.id = ai.attempt_section_id
                WHERE ase.attempt_id = :attemptId AND gj.grading_mode = 'AI'
                ORDER BY ai.id
                """, Map.of("attemptId", attemptId), (rs, rowNum) -> new JobState(
                rs.getObject("attempt_item_id", UUID.class), rs.getString("status"), rs.getBigDecimal("llm_band")));
    }
}
