package com.ieltspath.learning.infrastructure.persistence;

import com.ieltspath.learning.domain.repository.SubmissionHistoryRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Reads the learner's submissions from the tables that hold them; nothing is copied into a history table. Every branch of the
 * course query selects the same columns in the same order, so the branches stack with {@code UNION ALL}. A submission that
 * never counted is left out: an essay whose grading failed (the points went back), a speaking recording that was never sent
 * or was abandoned.
 */
@Repository
public class JdbcSubmissionHistoryRepository implements SubmissionHistoryRepository {
    private static final String COLUMNS = """
            kind, id, at, skill, task, stem, band, percent, correct_count, total_count, passed, status, lesson_id, topic_id,
            course_id, attempt_id, package_version_id, revision_no""";

    private static final RowMapper<Entry> ENTRY = JdbcSubmissionHistoryRepository::entry;

    private final JdbcTemplate jdbc;

    public JdbcSubmissionHistoryRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<Entry> findCourse(UUID userId, int limit, int offset) {
        return jdbc.query("SELECT " + COLUMNS + " FROM (" + """
                SELECT 'PRACTICE' AS kind, a.id, a.submitted_at AS at, a.skill AS skill, NULL::text AS task, NULL::text AS stem,
                       NULL::numeric AS band, NULL::double precision AS percent, a.correct_count, a.total_count, a.passed,
                       NULL::text AS status, a.lesson_id, lp.topic_id AS topic_id, NULL::uuid AS course_id, a.id AS attempt_id,
                       a.package_version_id, NULL::integer AS revision_no
                FROM practice_attempts a
                LEFT JOIN lesson_progress lp ON lp.user_id = a.user_id AND lp.lesson_id = a.lesson_id
                WHERE a.user_id = ? AND a.origin = 'LESSON' AND a.submitted_at IS NOT NULL
                UNION ALL
                SELECT 'WRITING', s.id, s.submitted_at, 'WRITING', s.prompt_snapshot->>'task', s.prompt_snapshot->>'stem',
                       s.overall_band, NULL, NULL, NULL, s.passed, s.status, s.lesson_id, lp.topic_id, NULL, NULL, NULL, s.revision_no
                FROM lesson_writing_submissions s
                LEFT JOIN lesson_progress lp ON lp.user_id = s.user_id AND lp.lesson_id = s.lesson_id
                WHERE s.user_id = ? AND s.source IN ('LESSON_BLOCK', 'PRACTICE_ITEM') AND s.status <> 'FAILED'
                UNION ALL
                SELECT 'SPEAKING', p.id, p.created_at, 'SPEAKING', NULL, NULL, p.overall_band, NULL, NULL, NULL, NULL, p.status,
                       p.lesson_id, lp.topic_id, NULL, NULL, p.package_version_id, NULL
                FROM speaking_submissions p
                LEFT JOIN lesson_progress lp ON lp.user_id = p.user_id AND lp.lesson_id = p.lesson_id
                WHERE p.user_id = ? AND p.lesson_id IS NOT NULL AND p.status NOT IN ('UPLOADING', 'ABANDONED')
                UNION ALL
                SELECT 'TOPIC_TEST', t.id, t.consumed_at, NULL, NULL, NULL, NULL, t.percent, NULL, NULL, NULL, NULL, NULL,
                       t.topic_id, NULL, t.consumed_attempt_id, t.package_version_id, NULL
                FROM topic_test_assignments t
                WHERE t.user_id = ? AND t.consumed_at IS NOT NULL
                UNION ALL
                SELECT 'COURSE_TEST', c.id, c.consumed_at, NULL, NULL, NULL, NULL, c.percent::double precision, NULL, NULL, NULL,
                       NULL, NULL, NULL, c.course_id, c.consumed_attempt_id, c.package_version_id, NULL
                FROM course_test_assignments c
                WHERE c.user_id = ? AND c.consumed_at IS NOT NULL
                UNION ALL
                SELECT 'PRACTICE_TEST', m.id, m.consumed_at, NULL, NULL, NULL, NULL, m.percent, NULL, NULL, NULL, NULL, NULL,
                       NULL, m.course_id, m.consumed_attempt_id, m.package_version_id, NULL
                FROM practice_test_assignments m
                WHERE m.user_id = ? AND m.consumed_at IS NOT NULL
                """ + ") history ORDER BY at DESC, id LIMIT ? OFFSET ?", ENTRY,
                userId, userId, userId, userId, userId, userId, limit, offset);
    }

    @Override
    public List<Entry> findFree(UUID userId, String skill, int limit, int offset) {
        if ("WRITING".equals(skill)) {
            return jdbc.query("""
                    SELECT 'WRITING' AS kind, s.id, s.submitted_at AS at, 'WRITING' AS skill, s.prompt_snapshot->>'task' AS task,
                           s.prompt_snapshot->>'stem' AS stem, s.overall_band AS band, NULL::double precision AS percent,
                           NULL::integer AS correct_count, NULL::integer AS total_count, s.passed, s.status,
                           NULL::uuid AS lesson_id, NULL::uuid AS topic_id, NULL::uuid AS course_id, NULL::uuid AS attempt_id,
                           NULL::uuid AS package_version_id, s.revision_no
                    FROM lesson_writing_submissions s
                    WHERE s.user_id = ? AND s.source = 'FREE_WRITING' AND s.status <> 'FAILED'
                    ORDER BY s.submitted_at DESC, s.id
                    LIMIT ? OFFSET ?
                    """, ENTRY, userId, limit, offset);
        }
        if ("SPEAKING".equals(skill)) {
            return jdbc.query("""
                    SELECT 'SPEAKING' AS kind, p.id, p.created_at AS at, 'SPEAKING' AS skill, NULL::text AS task, NULL::text AS stem,
                           p.overall_band AS band, NULL::double precision AS percent, NULL::integer AS correct_count,
                           NULL::integer AS total_count, NULL::boolean AS passed, p.status, NULL::uuid AS lesson_id,
                           NULL::uuid AS topic_id, NULL::uuid AS course_id, NULL::uuid AS attempt_id, p.package_version_id,
                           NULL::integer AS revision_no
                    FROM speaking_submissions p
                    WHERE p.user_id = ? AND p.lesson_id IS NULL AND p.status NOT IN ('UPLOADING', 'ABANDONED')
                    ORDER BY p.created_at DESC, p.id
                    LIMIT ? OFFSET ?
                    """, ENTRY, userId, limit, offset);
        }
        throw new IllegalArgumentException("skill must be WRITING or SPEAKING");
    }

    @Override
    public List<Entry> findKnowledgePractice(UUID userId, int limit, int offset) {
        return jdbc.query("""
                SELECT a.id, a.submitted_at, a.skill, a.correct_count, a.total_count, a.passed, a.knowledge_point_id
                FROM practice_attempts a
                WHERE a.user_id = ? AND a.origin = 'FREE' AND a.submitted_at IS NOT NULL
                ORDER BY a.submitted_at DESC, a.id
                LIMIT ? OFFSET ?
                """, (row, index) -> new Entry("KP_PRACTICE", row.getObject("id", UUID.class),
                row.getTimestamp("submitted_at").toInstant(), row.getString("skill"), null, null, null, null,
                nullableInt(row, "correct_count"), nullableInt(row, "total_count"), nullableBoolean(row, "passed"), null, null,
                null, null, row.getObject("id", UUID.class), null, null, row.getObject("knowledge_point_id", UUID.class),
                null, null), userId, limit, offset);
    }

    @Override
    public Optional<Instant> essaySubmittedAt(UUID userId, UUID submissionId) {
        return jdbc.query("SELECT submitted_at FROM lesson_writing_submissions WHERE id = ? AND user_id = ?",
                (row, index) -> row.getTimestamp("submitted_at").toInstant(), submissionId, userId).stream().findFirst();
    }

    @Override
    public List<PracticeTally> tallyKnowledgePractice(UUID userId, Collection<UUID> knowledgePointIds, Instant since) {
        if (knowledgePointIds.isEmpty()) return List.of();
        String marks = String.join(", ", Collections.nCopies(knowledgePointIds.size(), "?"));
        List<Object> args = new ArrayList<>();
        args.add(userId);
        args.add(Timestamp.from(since));
        args.addAll(knowledgePointIds);
        // The window counts every set of the point before DISTINCT ON keeps only its latest.
        return jdbc.query("""
                SELECT DISTINCT ON (knowledge_point_id) knowledge_point_id, attempts, id, submitted_at, correct_count,
                       total_count, passed
                FROM (
                    SELECT a.*, COUNT(*) OVER (PARTITION BY a.knowledge_point_id) AS attempts
                    FROM practice_attempts a
                    WHERE a.user_id = ? AND a.origin = 'FREE' AND a.submitted_at IS NOT NULL AND a.submitted_at >= ?
                      AND a.knowledge_point_id IN (%s)
                ) practised
                ORDER BY knowledge_point_id, submitted_at DESC, id
                """.formatted(marks), (row, index) -> new PracticeTally(row.getObject("knowledge_point_id", UUID.class),
                row.getInt("attempts"), row.getObject("id", UUID.class), row.getTimestamp("submitted_at").toInstant(),
                nullableInt(row, "correct_count"), nullableInt(row, "total_count"), nullableBoolean(row, "passed")),
                args.toArray());
    }

    private static Integer nullableInt(ResultSet row, String column) throws SQLException {
        int value = row.getInt(column);
        return row.wasNull() ? null : value;
    }

    private static Boolean nullableBoolean(ResultSet row, String column) throws SQLException {
        boolean value = row.getBoolean(column);
        return row.wasNull() ? null : value;
    }

    private static Entry entry(ResultSet row, int index) throws SQLException {
        Object percent = row.getObject("percent");
        Object correct = row.getObject("correct_count");
        Object total = row.getObject("total_count");
        Object passed = row.getObject("passed");
        Object revision = row.getObject("revision_no");
        return new Entry(row.getString("kind"), row.getObject("id", UUID.class), row.getTimestamp("at").toInstant(),
                row.getString("skill"), row.getString("task"), row.getString("stem"), row.getBigDecimal("band"),
                percent == null ? null : row.getDouble("percent"), correct == null ? null : row.getInt("correct_count"),
                total == null ? null : row.getInt("total_count"), passed == null ? null : row.getBoolean("passed"),
                row.getString("status"), row.getObject("lesson_id", UUID.class), row.getObject("topic_id", UUID.class),
                row.getObject("course_id", UUID.class), row.getObject("attempt_id", UUID.class),
                row.getObject("package_version_id", UUID.class), revision == null ? null : row.getInt("revision_no"));
    }
}
