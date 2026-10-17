package com.ieltspath.learning.infrastructure.persistence;

import com.ieltspath.learning.application.port.CourseScoreData;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcCourseScoreData implements CourseScoreData {
    private final NamedParameterJdbcTemplate jdbc;

    public JdbcCourseScoreData(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<Double> practiceAverage(UUID userId, UUID courseId) {
        // The lesson belongs to the course through the topic the learner opened it in.
        return Optional.ofNullable(jdbc.queryForObject("""
                SELECT avg(pa.correct_count::float8 / pa.total_count)
                FROM practice_attempts pa
                JOIN lesson_progress lp ON lp.user_id = pa.user_id AND lp.lesson_id = pa.lesson_id
                JOIN topic_progress tp ON tp.user_id = lp.user_id AND tp.topic_id = lp.topic_id
                WHERE pa.user_id = :userId AND tp.course_id = :courseId AND pa.counted_as_evidence
                  AND pa.submitted_at IS NOT NULL AND pa.total_count > 0
                """, new MapSqlParameterSource("userId", userId).addValue("courseId", courseId), Double.class));
    }

    @Override
    public List<Double> practiceTestPercents(UUID userId, UUID courseId) {
        return jdbc.queryForList("""
                SELECT percent / 100.0 FROM practice_test_assignments
                WHERE user_id = :userId AND course_id = :courseId AND consumed_at IS NOT NULL
                """, new MapSqlParameterSource("userId", userId).addValue("courseId", courseId), Double.class);
    }

    @Override
    public Optional<FinalTest> latestFinal(UUID userId, UUID courseId) {
        return jdbc.query("""
                SELECT consumed_attempt_id, percent FROM course_test_assignments
                WHERE user_id = :userId AND course_id = :courseId AND consumed_at IS NOT NULL
                ORDER BY consumed_at DESC, id LIMIT 1
                """, new MapSqlParameterSource("userId", userId).addValue("courseId", courseId), (row, index) ->
                new FinalTest(row.getObject("consumed_attempt_id", UUID.class),
                        row.getBigDecimal("percent").doubleValue() / 100.0)).stream().findFirst();
    }

    @Override
    public List<ErrorCount> topErrors(UUID userId, UUID attemptId, int limit) {
        return jdbc.query("""
                SELECT error_code, count(*) AS occurrences FROM question_attempt_events
                WHERE user_id = :userId AND attempt_id = :attemptId AND error_code IS NOT NULL AND correct = FALSE
                GROUP BY error_code ORDER BY occurrences DESC, error_code LIMIT :limit
                """, new MapSqlParameterSource("userId", userId).addValue("attemptId", attemptId)
                .addValue("limit", limit), (row, index) -> new ErrorCount(row.getString("error_code"),
                row.getInt("occurrences")));
    }
}
