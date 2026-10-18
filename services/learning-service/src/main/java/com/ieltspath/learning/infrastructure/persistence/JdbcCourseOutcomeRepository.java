package com.ieltspath.learning.infrastructure.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.learning.domain.repository.CourseOutcomeRepository;
import com.ieltspath.learning.domain.vo.CourseOutcome;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Array;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
public class JdbcCourseOutcomeRepository implements CourseOutcomeRepository {
    private static final TypeReference<Map<String, Double>> COMPONENTS = new TypeReference<>() { };

    private final NamedParameterJdbcTemplate jdbc;
    private final ObjectMapper json;

    public JdbcCourseOutcomeRepository(NamedParameterJdbcTemplate jdbc, ObjectMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    @Override
    public Optional<CourseOutcome> find(UUID userId, UUID courseId) {
        return jdbc.query("""
                SELECT o.status, o.total, o.components::text AS components, o.final_attempt_id, o.top_error_codes,
                       o.review_ids, o.decided_at,
                       EXISTS (SELECT 1 FROM review_items r WHERE r.id = ANY (o.review_ids)
                               AND r.status = 'PENDING') AS reviews_pending
                FROM course_outcomes o WHERE o.user_id = :userId AND o.course_id = :courseId
                """, new MapSqlParameterSource("userId", userId).addValue("courseId", courseId), (row, index) ->
                new CourseOutcome(userId, courseId, CourseOutcome.Status.valueOf(row.getString("status")),
                        row.getBigDecimal("total").doubleValue(), components(row.getString("components")),
                        row.getObject("final_attempt_id", UUID.class), strings(row.getArray("top_error_codes")),
                        SqlArrays.uuids(row.getArray("review_ids")), row.getTimestamp("decided_at").toInstant(),
                        row.getBoolean("reviews_pending"))).stream().findFirst();
    }

    @Override
    public void save(CourseOutcome outcome) {
        jdbc.update("""
                INSERT INTO course_outcomes (user_id, course_id, status, total, components, final_attempt_id,
                    top_error_codes, review_ids, decided_at)
                VALUES (:userId, :courseId, :status, :total, CAST(:components AS jsonb), :attemptId,
                    CAST(:errors AS varchar[]), CAST(:reviewIds AS uuid[]), :decidedAt)
                ON CONFLICT (user_id, course_id) DO UPDATE SET status = EXCLUDED.status, total = EXCLUDED.total,
                    components = EXCLUDED.components, final_attempt_id = EXCLUDED.final_attempt_id,
                    top_error_codes = EXCLUDED.top_error_codes, review_ids = EXCLUDED.review_ids,
                    decided_at = EXCLUDED.decided_at
                """, new MapSqlParameterSource().addValue("userId", outcome.userId())
                .addValue("courseId", outcome.courseId()).addValue("status", outcome.status().name())
                .addValue("total", outcome.total()).addValue("components", write(outcome.components()))
                .addValue("attemptId", outcome.finalAttemptId())
                .addValue("errors", outcome.topErrorCodes().stream()
                        .map(code -> "\"" + code.replace("\\", "\\\\").replace("\"", "\\\"") + "\"")
                        .collect(Collectors.joining(",", "{", "}")))
                .addValue("reviewIds", uuidArray(outcome.reviewIds()))
                .addValue("decidedAt", Timestamp.from(outcome.decidedAt())));
    }

    private Map<String, Double> components(String value) {
        try {
            return json.readValue(value, COMPONENTS);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Stored course components are not valid", exception);
        }
    }

    private String write(Map<String, Double> components) {
        try {
            return json.writeValueAsString(components);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Cannot store course components", exception);
        }
    }

    private static List<String> strings(Array array) throws SQLException {
        try {
            return Arrays.stream((Object[]) array.getArray()).map(Object::toString).toList();
        } finally {
            array.free();
        }
    }

    static String uuidArray(Collection<UUID> ids) {
        return ids.stream().map(UUID::toString).collect(Collectors.joining(",", "{", "}"));
    }
}
