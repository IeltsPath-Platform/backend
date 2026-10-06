package com.group01.learning.infrastructure.persistence;

import com.group01.learning.domain.aggregate.CourseTestAssignment;
import com.group01.learning.domain.repository.CourseTestAssignmentRepository;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcCourseTestAssignmentRepository implements CourseTestAssignmentRepository {
    private static final String COLUMNS = "id, user_id, course_id, package_id, package_version_id, "
            + "consumed_attempt_id, percent";
    private static final RowMapper<CourseTestAssignment> ASSIGNMENT = (row, index) ->
            CourseTestAssignment.restore(row.getObject("id", UUID.class), row.getObject("user_id", UUID.class),
                    row.getObject("course_id", UUID.class), row.getObject("package_id", UUID.class),
                    row.getObject("package_version_id", UUID.class), row.getObject("consumed_attempt_id", UUID.class),
                    row.getBigDecimal("percent"));

    private final NamedParameterJdbcTemplate jdbc;

    public JdbcCourseTestAssignmentRepository(NamedParameterJdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    public Optional<CourseTestAssignment> findOpen(UUID userId, UUID courseId) {
        return jdbc.query("SELECT " + COLUMNS + " FROM course_test_assignments "
                        + "WHERE user_id = :userId AND course_id = :courseId AND consumed_at IS NULL",
                Map.of("userId", userId, "courseId", courseId), ASSIGNMENT).stream().findFirst();
    }

    @Override
    public Optional<CourseTestAssignment> findOpenForAttempt(UUID userId, UUID packageVersionId, Instant completedAt) {
        return jdbc.query("SELECT " + COLUMNS + " FROM course_test_assignments "
                        + "WHERE user_id = :userId AND package_version_id = :versionId AND consumed_at IS NULL "
                        + "AND assigned_at <= :completedAt ORDER BY assigned_at DESC LIMIT 1 FOR UPDATE",
                Map.of("userId", userId, "versionId", packageVersionId,
                        "completedAt", Timestamp.from(completedAt)), ASSIGNMENT).stream().findFirst();
    }

    @Override
    public Map<UUID, Instant> lastConsumedAt(UUID userId, UUID courseId) {
        Map<UUID, Instant> latest = new HashMap<>();
        jdbc.query("""
                SELECT package_id, max(consumed_at) AS last_consumed FROM course_test_assignments
                WHERE user_id = :userId AND course_id = :courseId AND consumed_at IS NOT NULL GROUP BY package_id
                """, Map.of("userId", userId, "courseId", courseId), (RowCallbackHandler) row -> latest.put(
                row.getObject("package_id", UUID.class), row.getTimestamp("last_consumed").toInstant()));
        return Map.copyOf(latest);
    }

    @Override
    public void save(CourseTestAssignment assignment) {
        jdbc.update("""
                INSERT INTO course_test_assignments (id, user_id, course_id, package_id, package_version_id,
                assigned_at, consumed_attempt_id, consumed_at, percent)
                VALUES (:id, :userId, :courseId, :packageId, :versionId, clock_timestamp(), :attemptId,
                CASE WHEN :consumed THEN clock_timestamp() END, :percent)
                ON CONFLICT (id) DO UPDATE SET consumed_attempt_id = EXCLUDED.consumed_attempt_id,
                consumed_at = EXCLUDED.consumed_at, percent = EXCLUDED.percent
                WHERE course_test_assignments.consumed_at IS NULL
                """, new MapSqlParameterSource().addValue("id", assignment.id())
                .addValue("userId", assignment.userId()).addValue("courseId", assignment.courseId())
                .addValue("packageId", assignment.packageId()).addValue("versionId", assignment.packageVersionId())
                .addValue("attemptId", assignment.consumedAttemptId()).addValue("consumed", assignment.isConsumed())
                .addValue("percent", assignment.percent() == null ? null :
                        assignment.percent().setScale(2, java.math.RoundingMode.HALF_UP)));
    }
}
