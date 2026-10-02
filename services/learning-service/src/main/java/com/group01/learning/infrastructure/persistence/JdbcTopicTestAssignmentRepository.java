package com.group01.learning.infrastructure.persistence;

import com.group01.learning.domain.aggregate.TopicTestAssignment;
import com.group01.learning.domain.repository.TopicTestAssignmentRepository;
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
public class JdbcTopicTestAssignmentRepository implements TopicTestAssignmentRepository {
    private static final String COLUMNS = "id, user_id, topic_id, package_id, package_version_id, consumed_attempt_id, percent";
    private static final RowMapper<TopicTestAssignment> ASSIGNMENT = (row, index) -> {
        Object percent = row.getObject("percent");
        return TopicTestAssignment.restore(row.getObject("id", UUID.class), row.getObject("user_id", UUID.class),
                row.getObject("topic_id", UUID.class), row.getObject("package_id", UUID.class),
                row.getObject("package_version_id", UUID.class), row.getObject("consumed_attempt_id", UUID.class),
                percent == null ? null : BigDecimal.valueOf(((Number) percent).doubleValue()));
    };

    private final NamedParameterJdbcTemplate jdbc;

    public JdbcTopicTestAssignmentRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<TopicTestAssignment> findOpen(UUID userId, UUID topicId) {
        return jdbc.query("SELECT " + COLUMNS + """
                 FROM topic_test_assignments
                WHERE user_id = :userId AND topic_id = :topicId AND consumed_at IS NULL
                """, Map.of("userId", userId, "topicId", topicId), ASSIGNMENT).stream().findFirst();
    }

    @Override
    public Optional<TopicTestAssignment> findOpenForAttempt(UUID userId, UUID packageVersionId, Instant completedAt) {
        return jdbc.query("SELECT " + COLUMNS + """
                 FROM topic_test_assignments
                WHERE user_id = :userId AND package_version_id = :versionId AND consumed_at IS NULL
                  AND assigned_at <= :completedAt
                ORDER BY assigned_at DESC LIMIT 1
                FOR UPDATE
                """, Map.of("userId", userId, "versionId", packageVersionId, "completedAt", Timestamp.from(completedAt)),
                ASSIGNMENT).stream().findFirst();
    }

    @Override
    public Map<UUID, Instant> lastConsumedAt(UUID userId, UUID topicId) {
        Map<UUID, Instant> latest = new HashMap<>();
        jdbc.query("""
                SELECT package_id, max(consumed_at) AS last_consumed FROM topic_test_assignments
                WHERE user_id = :userId AND topic_id = :topicId AND consumed_at IS NOT NULL GROUP BY package_id
                """, Map.of("userId", userId, "topicId", topicId), (RowCallbackHandler) row -> latest.put(
                row.getObject("package_id", UUID.class), row.getTimestamp("last_consumed").toInstant()));
        return latest;
    }

    @Override
    public void save(TopicTestAssignment assignment) {
        jdbc.update("""
                INSERT INTO topic_test_assignments (id, user_id, topic_id, package_id, package_version_id, assigned_at,
                consumed_attempt_id, consumed_at, percent)
                VALUES (:id, :userId, :topicId, :packageId, :versionId, clock_timestamp(),
                :attemptId, CASE WHEN :consumed THEN clock_timestamp() END, :percent)
                ON CONFLICT (id) DO UPDATE SET consumed_attempt_id = EXCLUDED.consumed_attempt_id,
                consumed_at = EXCLUDED.consumed_at, percent = EXCLUDED.percent
                WHERE topic_test_assignments.consumed_at IS NULL
                """, new MapSqlParameterSource()
                .addValue("id", assignment.id())
                .addValue("userId", assignment.userId())
                .addValue("topicId", assignment.topicId())
                .addValue("packageId", assignment.packageId())
                .addValue("versionId", assignment.packageVersionId())
                .addValue("attemptId", assignment.consumedAttemptId())
                .addValue("consumed", assignment.isConsumed())
                .addValue("percent", assignment.percent() == null ? null : assignment.percent().doubleValue()));
    }
}
