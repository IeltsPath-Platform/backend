package com.ieltspath.assessment.infrastructure.persistence.adapter;

import com.ieltspath.assessment.application.port.GateEssayJobStore;
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
        enqueueHuman(submissionId, userId, idempotencyKey, "LLM_FAILED");
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void enqueueHuman(UUID submissionId, UUID userId, String idempotencyKey, String reason) {
        String sql = """
                INSERT INTO grading_jobs (id, submission_id, user_id, skill, grading_mode, status,
                                          point_cost_snapshot, idempotency_key, created_at, reason)
                SELECT gen_random_uuid(), id, user_id, skill, 'HUMAN', 'QUEUED', NULL, :key, CURRENT_TIMESTAMP, :reason
                FROM learner_submissions
                WHERE id = :submissionId AND user_id = :userId AND status = 'SUBMITTED'
                ON CONFLICT (idempotency_key) DO NOTHING
                RETURNING id
                """;
        List<UUID> inserted = jdbc.query(sql, new MapSqlParameterSource().addValue("submissionId", submissionId)
                        .addValue("userId", userId).addValue("key", idempotencyKey).addValue("reason", reason),
                (rs, rowNum) -> rs.getObject("id", UUID.class));
        if (!inserted.isEmpty()) {
            jdbc.update("INSERT INTO human_reviews (id, grading_job_id, status) VALUES (gen_random_uuid(), :jobId, 'QUEUED')",
                    Map.of("jobId", inserted.getFirst()));
        }
    }

    @Override
    public boolean jobExists(String idempotencyKey) {
        Integer found = jdbc.queryForObject("SELECT count(*) FROM grading_jobs WHERE idempotency_key = :key",
                Map.of("key", idempotencyKey), Integer.class);
        return found != null && found > 0;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void enqueueCourseHuman(Collection<UUID> essayItemIds, UUID userId, Map<UUID, UUID> submitted) {
        if (essayItemIds.isEmpty()) return;
        var missing=essayItemIds.stream().filter(id->!submitted.containsKey(id)).toList();
        var args=new MapSqlParameterSource().addValue("ids",essayItemIds).addValue("user",userId)
                .addValue("missing",missing.isEmpty()?List.of(new UUID(0,0)):missing)
                .addValue("frozen",submitted.isEmpty()?List.of(new UUID(0,0)):submitted.values());
        jdbc.update("""
                INSERT INTO learner_submissions(id,user_id,attempt_item_id,prompt_snapshot,skill,text_payload,status,submission_key)
                SELECT gen_random_uuid(),:user,i.id,
                  jsonb_build_object('stem',i.question_snapshot->>'stem','task',i.answer_snapshot->'answerSpec'->>'task','omitted',true),
                  'WRITING','','SUBMITTED','course-omitted:'||i.id
                FROM attempt_items i WHERE i.id IN (:missing)
                ON CONFLICT(user_id,submission_key) DO NOTHING
                """,args);
        jdbc.update("""
                INSERT INTO grading_jobs(id,submission_id,user_id,skill,grading_mode,status,idempotency_key,reason)
                SELECT gen_random_uuid(),l.id,:user,'WRITING','HUMAN','QUEUED','gate-essay:'||l.attempt_item_id||':human','COURSE_GATE_POLICY'
                FROM (SELECT DISTINCT ON(attempt_item_id) id,attempt_item_id FROM learner_submissions
                  WHERE attempt_item_id IN (:ids) AND user_id=:user AND status='SUBMITTED' AND skill='WRITING'
                  AND (id IN (:frozen) OR (attempt_item_id IN (:missing) AND submission_key='course-omitted:'||attempt_item_id))
                  AND text_payload IS NOT NULL ORDER BY attempt_item_id,submitted_at DESC,id DESC) l
                ON CONFLICT(idempotency_key) DO NOTHING
                """,args);
        jdbc.update("""
                INSERT INTO human_reviews(id,grading_job_id,status)
                SELECT gen_random_uuid(),j.id,'QUEUED' FROM grading_jobs j JOIN learner_submissions l ON l.id=j.submission_id
                WHERE l.attempt_item_id IN (:ids) AND j.user_id=:user AND j.grading_mode='HUMAN'
                ON CONFLICT(grading_job_id) DO NOTHING
                """,args);
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
                      AND aa.attempt_type IN ('TOPIC_GATE', 'COURSE_GATE', 'PLACEMENT', 'OFFICIAL_PRACTICE')
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
    public void complete(UUID jobId, BigDecimal band, String feedback, Instant now) {
        jdbc.update("""
                UPDATE grading_jobs SET status = 'COMPLETED', llm_band = :band, llm_feedback = CAST(:feedback AS JSONB),
                    completed_at = :now, processing_started_at = NULL
                WHERE id = :id AND grading_mode = 'AI' AND status = 'PROCESSING'
                """, new MapSqlParameterSource().addValue("id", jobId).addValue("band", band)
                .addValue("feedback", feedback).addValue("now", Timestamp.from(now)));
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void complete(UUID jobId, BigDecimal band, String feedback, String promptVersion, Instant now) {
        jdbc.update("""
                UPDATE grading_jobs SET status = 'COMPLETED', llm_band = :band, llm_feedback = CAST(:feedback AS JSONB),
                    prompt_version = :promptVersion, completed_at = :now, processing_started_at = NULL
                WHERE id = :id AND grading_mode = 'AI' AND status = 'PROCESSING'
                """, new MapSqlParameterSource().addValue("id", jobId).addValue("band", band)
                .addValue("feedback", feedback).addValue("promptVersion", promptVersion).addValue("now", Timestamp.from(now)));
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void recordComparison(UUID jobId) {
            jdbc.update("""
                    INSERT INTO essay_grade_comparisons(submission_id,attempt_type,task,llm_band,llm_criteria,llm_prompt_version)
                    SELECT j.submission_id,a.attempt_type,i.answer_snapshot->'answerSpec'->>'task',j.llm_band,
                           COALESCE(j.llm_feedback->'criteria','[]'::jsonb),j.prompt_version
                    FROM grading_jobs j JOIN learner_submissions l ON l.id=j.submission_id
                    JOIN attempt_items i ON i.id=l.attempt_item_id JOIN attempt_sections s ON s.id=i.attempt_section_id
                    JOIN assessment_attempts a ON a.id=s.attempt_id WHERE j.id=:id AND j.status='COMPLETED'
                      AND j.grading_mode='AI'
                    ON CONFLICT(submission_id) DO UPDATE SET llm_band=EXCLUDED.llm_band,
                      llm_criteria=EXCLUDED.llm_criteria,llm_prompt_version=EXCLUDED.llm_prompt_version
                    """, Map.of("id",jobId));
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
                SELECT ai.id AS attempt_item_id, gj.status, gj.llm_band, gj.llm_feedback::text AS llm_feedback
                FROM grading_jobs gj
                JOIN learner_submissions ls ON ls.id = gj.submission_id
                JOIN attempt_items ai ON ai.id = ls.attempt_item_id
                JOIN attempt_sections ase ON ase.id = ai.attempt_section_id
                WHERE ase.attempt_id = :attemptId AND gj.grading_mode = 'AI'
                ORDER BY ai.id
                """, Map.of("attemptId", attemptId), (rs, rowNum) -> new JobState(
                rs.getObject("attempt_item_id", UUID.class), rs.getString("status"), rs.getBigDecimal("llm_band"),
                rs.getString("llm_feedback")));
    }
}
