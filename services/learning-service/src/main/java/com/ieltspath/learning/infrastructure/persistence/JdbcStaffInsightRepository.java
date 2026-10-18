package com.ieltspath.learning.infrastructure.persistence;

import com.ieltspath.learning.domain.repository.StaffInsightRepository;
import com.ieltspath.learning.domain.service.AnomalyRules;
import com.ieltspath.learning.domain.vo.StaffRecords.EdgeStat;
import com.ieltspath.learning.domain.vo.StaffRecords.FlagKind;
import com.ieltspath.learning.domain.vo.StaffRecords.FlagStatus;
import com.ieltspath.learning.domain.vo.StaffRecords.KnowledgePointStat;
import com.ieltspath.learning.domain.vo.StaffRecords.LearnerFlag;
import com.ieltspath.learning.domain.vo.StaffRecords.QuestionStat;
import com.ieltspath.learning.domain.vo.StaffRecords.Queue;
import com.ieltspath.learning.domain.vo.StaffRecords.WorkItem;
import com.ieltspath.learning.domain.vo.StaffRecords.WorkKind;
import com.ieltspath.learning.domain.vo.StaffRecords.WorkStatus;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Array;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcStaffInsightRepository implements StaffInsightRepository {
    private static final String FLAG_COLUMNS = """
            id, user_id, kind, evidence::text AS evidence, status, reviewed_by, note, created_at, reviewed_at""";
    private static final String ITEM_COLUMNS = """
            id, queue, kind, subject_user_id, subject_ref, detail::text AS detail, status, assignee_id, action, result,
            note, note_visible_to_learner, created_at, updated_at""";

    private static final RowMapper<LearnerFlag> FLAG = (row, index) -> new LearnerFlag(row.getObject("id", UUID.class),
            row.getObject("user_id", UUID.class), FlagKind.valueOf(row.getString("kind")), row.getString("evidence"),
            FlagStatus.valueOf(row.getString("status")), row.getObject("reviewed_by", UUID.class),
            row.getString("note"), row.getTimestamp("created_at").toInstant(), instant(row.getTimestamp("reviewed_at")));

    private static final RowMapper<WorkItem> ITEM = (row, index) -> new WorkItem(row.getObject("id", UUID.class),
            Queue.valueOf(row.getString("queue")), WorkKind.valueOf(row.getString("kind")),
            row.getObject("subject_user_id", UUID.class), row.getObject("subject_ref", UUID.class),
            row.getString("detail"), WorkStatus.valueOf(row.getString("status")),
            row.getObject("assignee_id", UUID.class), row.getString("action"), row.getString("result"),
            row.getString("note"), row.getBoolean("note_visible_to_learner"),
            row.getTimestamp("created_at").toInstant(), row.getTimestamp("updated_at").toInstant());

    private final NamedParameterJdbcTemplate jdbc;

    public JdbcStaffInsightRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static Instant instant(Timestamp value) {
        return value == null ? null : value.toInstant();
    }

    private static MapSqlParameterSource params(String name, Object value) {
        return new MapSqlParameterSource(name, value);
    }

    @Override
    public List<AnomalyRules.Sample> samplesSince(UUID userId, Instant since) {
        return jdbc.query("""
                SELECT duration_ms, correct FROM question_attempt_events
                WHERE user_id = :userId AND occurred_at >= :since
                """, params("userId", userId).addValue("since", Timestamp.from(since)), (row, index) -> {
            long duration = row.getLong("duration_ms");
            Long durationMs = row.wasNull() ? null : duration;
            boolean correct = row.getBoolean("correct");
            return new AnomalyRules.Sample(durationMs, row.wasNull() ? null : correct);
        });
    }

    @Override
    public long eventsSince(UUID userId, Instant since) {
        Long count = jdbc.queryForObject("""
                SELECT count(*) FROM question_attempt_events WHERE user_id = :userId AND occurred_at >= :since
                """, params("userId", userId).addValue("since", Timestamp.from(since)), Long.class);
        return count == null ? 0 : count;
    }

    @Override
    public long otherLearnersWithEssayHash(UUID userId, String essayHash) {
        Long count = jdbc.queryForObject("""
                SELECT count(DISTINCT user_id) FROM lesson_writing_submissions
                WHERE essay_hash = :hash AND user_id <> :userId
                """, params("hash", essayHash).addValue("userId", userId), Long.class);
        return count == null ? 0 : count;
    }

    @Override
    public List<String> recentEssayHashes(UUID userId, Instant since) {
        return jdbc.queryForList("""
                SELECT DISTINCT essay_hash FROM lesson_writing_submissions
                WHERE user_id = :userId AND essay_hash IS NOT NULL AND submitted_at >= :since
                """, params("userId", userId).addValue("since", Timestamp.from(since)), String.class);
    }

    @Override
    public boolean insertFlag(UUID userId, FlagKind kind, String evidenceJson, Instant notBefore) {
        return jdbc.update("""
                INSERT INTO learner_flags (id, user_id, kind, evidence)
                SELECT :id, :userId, :kind, CAST(:evidence AS jsonb)
                WHERE NOT EXISTS (SELECT 1 FROM learner_flags WHERE user_id = :userId AND kind = :kind
                                  AND created_at >= :notBefore)
                """, params("id", UUID.randomUUID()).addValue("userId", userId).addValue("kind", kind.name())
                .addValue("evidence", evidenceJson).addValue("notBefore", Timestamp.from(notBefore))) == 1;
    }

    @Override
    public int openInactivityFlags(Instant cutoff, Instant now) {
        // One statement: the learners and their last answer come from the event log, the open course from topic
        // progress, and an open flag already on record stops a second one.
        return jdbc.update("""
                INSERT INTO learner_flags (id, user_id, kind, evidence)
                SELECT gen_random_uuid(), activity.user_id, 'INACTIVE',
                       jsonb_build_object('lastAnswerAt', activity.last_at, 'raisedAt', CAST(:now AS text))
                FROM (SELECT user_id, max(occurred_at) AS last_at FROM question_attempt_events GROUP BY user_id) activity
                WHERE activity.last_at <= :cutoff
                  AND EXISTS (SELECT 1 FROM topic_progress topic WHERE topic.user_id = activity.user_id
                              AND topic.course_id IS NOT NULL
                              AND NOT EXISTS (SELECT 1 FROM course_progress passed
                                              WHERE passed.user_id = topic.user_id AND passed.course_id = topic.course_id))
                  AND NOT EXISTS (SELECT 1 FROM learner_flags open_flag WHERE open_flag.user_id = activity.user_id
                                  AND open_flag.kind = 'INACTIVE' AND open_flag.status = 'OPEN')
                """, new MapSqlParameterSource().addValue("cutoff", Timestamp.from(cutoff))
                .addValue("now", now.toString()));
    }

    @Override
    public int dismissInactivityFlags(UUID userId) {
        return jdbc.update("""
                UPDATE learner_flags SET status = 'DISMISSED', reviewed_at = CURRENT_TIMESTAMP,
                    note = 'learner answered again'
                WHERE user_id = :userId AND kind = 'INACTIVE' AND status = 'OPEN'
                """, new MapSqlParameterSource("userId", userId));
    }

    @Override
    public Optional<LearnerFlag> findFlag(UUID id) {
        return jdbc.query("SELECT " + FLAG_COLUMNS + " FROM learner_flags WHERE id = :id", params("id", id), FLAG)
                .stream().findFirst();
    }

    @Override
    public boolean reviewFlag(UUID id, FlagStatus status, UUID reviewer, String note) {
        return jdbc.update("""
                UPDATE learner_flags SET status = :status, reviewed_by = :reviewer, note = :note,
                reviewed_at = clock_timestamp() WHERE id = :id
                """, params("id", id).addValue("status", status.name()).addValue("reviewer", reviewer)
                .addValue("note", note)) == 1;
    }

    @Override
    public List<LearnerFlag> flagsOf(UUID userId) {
        return jdbc.query("SELECT " + FLAG_COLUMNS + " FROM learner_flags WHERE user_id = :userId "
                + "ORDER BY created_at DESC", params("userId", userId), FLAG);
    }

    @Override
    public List<LearnerFlag> openFlags() {
        return jdbc.query("SELECT " + FLAG_COLUMNS + " FROM learner_flags WHERE status = 'OPEN' ORDER BY created_at",
                new MapSqlParameterSource(), FLAG);
    }

    @Override
    public boolean addWorkItem(Queue queue, WorkKind kind, UUID subjectUserId, UUID subjectRef, String detailJson) {
        return jdbc.update("""
                INSERT INTO work_items (id, queue, kind, subject_user_id, subject_ref, detail)
                VALUES (:id, :queue, :kind, :userId, :ref, CAST(:detail AS jsonb))
                ON CONFLICT (kind, subject_ref) DO NOTHING
                """, params("id", UUID.randomUUID()).addValue("queue", queue.name()).addValue("kind", kind.name())
                .addValue("userId", subjectUserId).addValue("ref", subjectRef).addValue("detail", detailJson)) == 1;
    }

    @Override
    public Optional<WorkItem> findWorkItem(UUID id) {
        return jdbc.query("SELECT " + ITEM_COLUMNS + " FROM work_items WHERE id = :id", params("id", id), ITEM)
                .stream().findFirst();
    }

    @Override
    public List<WorkItem> workItems(Collection<Queue> queues, WorkStatus status) {
        if (queues.isEmpty()) return List.of();
        return jdbc.query("SELECT " + ITEM_COLUMNS + " FROM work_items WHERE queue IN (:queues) "
                        + "AND (CAST(:status AS varchar) IS NULL OR status = :status) ORDER BY created_at, id",
                params("queues", queues.stream().map(Enum::name).toList())
                        .addValue("status", status == null ? null : status.name()), ITEM);
    }

    @Override
    public List<WorkItem> openWorkItemsOf(UUID userId) {
        return jdbc.query("SELECT " + ITEM_COLUMNS + " FROM work_items WHERE subject_user_id = :userId "
                + "AND status <> 'DONE' ORDER BY created_at", params("userId", userId), ITEM);
    }

    @Override
    public List<WorkItem> visibleNotes(UUID userId) {
        return jdbc.query("SELECT " + ITEM_COLUMNS + " FROM work_items WHERE subject_user_id = :userId "
                + "AND note_visible_to_learner AND note IS NOT NULL ORDER BY updated_at DESC",
                params("userId", userId), ITEM);
    }

    @Override
    public void updateWorkItem(UUID id, WorkStatus status, UUID assignee, String action, String result, String note,
                               boolean noteVisibleToLearner) {
        jdbc.update("""
                UPDATE work_items SET status = :status, assignee_id = :assignee, action = :action, result = :result,
                note = :note, note_visible_to_learner = :visible, updated_at = clock_timestamp() WHERE id = :id
                """, params("id", id).addValue("status", status.name()).addValue("assignee", assignee)
                .addValue("action", action).addValue("result", result).addValue("note", note)
                .addValue("visible", noteVisibleToLearner));
    }

    @Override
    public List<OpenRemediation> openRemediation() {
        return jdbc.query("""
                SELECT f.id, f.user_id, f.course_id, f.topic_id, f.kind FROM remediation_flags f
                WHERE EXISTS (SELECT 1 FROM review_items r WHERE r.id = ANY (f.review_ids) AND r.status = 'PENDING')
                """, new MapSqlParameterSource(), (row, index) -> new OpenRemediation(row.getObject("id", UUID.class),
                row.getObject("user_id", UUID.class), row.getObject("course_id", UUID.class),
                row.getObject("topic_id", UUID.class), row.getString("kind")));
    }

    @Override
    public List<UUID> contentGapKnowledgePoints() {
        return jdbc.queryForList("SELECT kp_id FROM content_gap_signals ORDER BY last_detected_at DESC",
                new MapSqlParameterSource(), UUID.class);
    }

    @Override
    public List<QuestionStat> questionStats(int minAttempts, int limit) {
        return jdbc.query("""
                SELECT question_version_id, count(*) AS attempts, count(*) FILTER (WHERE correct) AS right_count,
                       percentile_cont(0.5) WITHIN GROUP (ORDER BY duration_ms) AS median_ms,
                       (SELECT array_agg(code ORDER BY n DESC, code) FROM (
                            SELECT e2.error_code AS code, count(*) AS n FROM question_attempt_events e2
                            WHERE e2.question_version_id = e.question_version_id AND e2.first_submission
                              AND e2.error_code IS NOT NULL GROUP BY e2.error_code ORDER BY n DESC, code LIMIT 3) top)
                       AS top_errors
                FROM question_attempt_events e
                WHERE first_submission AND correct IS NOT NULL
                GROUP BY question_version_id HAVING count(*) >= :min
                ORDER BY count(*) DESC, question_version_id LIMIT :limit
                """, params("min", minAttempts).addValue("limit", limit), (row, index) -> {
            double median = row.getDouble("median_ms");
            Long medianMs = row.wasNull() ? null : Math.round(median);
            return new QuestionStat(row.getObject("question_version_id", UUID.class), row.getInt("attempts"),
                    row.getInt("right_count"), medianMs, strings(row.getArray("top_errors")));
        });
    }

    @Override
    public List<EdgeStat> edgeStats() {
        // Probe of prerequisite kp_id made while its parent_kp_id was failing; only concluded diagnoses count.
        return jdbc.query("""
                SELECT p.kp_id AS from_kp, p.parent_kp_id AS to_kp, count(*) AS probes,
                       count(*) FILTER (WHERE p.passed) AS passed,
                       count(*) FILTER (WHERE p.kp_id = ANY (s.conclusion_kp_ids)) AS as_root
                FROM diagnostic_probes p JOIN diagnostic_sessions s ON s.id = p.session_id
                WHERE s.status = 'CONCLUDED' AND p.status = 'ANSWERED'
                GROUP BY p.kp_id, p.parent_kp_id ORDER BY p.kp_id, p.parent_kp_id
                """, new MapSqlParameterSource(), (row, index) -> new EdgeStat(row.getObject("from_kp", UUID.class),
                row.getObject("to_kp", UUID.class), row.getInt("probes"), row.getInt("passed"),
                row.getInt("as_root")));
    }

    @Override
    public List<KnowledgePointStat> knowledgePointStats() {
        return jdbc.query("""
                SELECT m.knowledge_point_id AS kp, count(*) AS learners, avg(m.mastery) AS average,
                       coalesce(max(r.times_root), 0) AS times_root, coalesce(max(r.probes), 0) AS probes,
                       coalesce(max(r.failed), 0) AS failed
                FROM kp_mastery_profiles m
                LEFT JOIN (SELECT p.kp_id, count(*) FILTER (WHERE p.kp_id = ANY (s.conclusion_kp_ids)) AS times_root,
                                  count(*) AS probes, count(*) FILTER (WHERE NOT p.passed) AS failed
                           FROM diagnostic_probes p JOIN diagnostic_sessions s ON s.id = p.session_id
                           WHERE s.status = 'CONCLUDED' AND p.status = 'ANSWERED' GROUP BY p.kp_id) r
                       ON r.kp_id = m.knowledge_point_id
                GROUP BY m.knowledge_point_id ORDER BY m.knowledge_point_id
                """, new MapSqlParameterSource(), (row, index) -> new KnowledgePointStat(row.getObject("kp", UUID.class),
                row.getInt("learners"), row.getDouble("average"), row.getInt("times_root"), row.getInt("probes"),
                row.getInt("failed")));
    }

    private static List<String> strings(Array array) throws SQLException {
        if (array == null) return List.of();
        try {
            return Arrays.stream((Object[]) array.getArray()).map(Object::toString).toList();
        } finally {
            array.free();
        }
    }
}
