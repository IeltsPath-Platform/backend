package com.ieltspath.learning.infrastructure.persistence;

import com.ieltspath.learning.application.port.LearningProgressReader;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.*;

@Repository
public class JdbcLearningProgressReader implements LearningProgressReader {
    private final NamedParameterJdbcTemplate jdbc;
    public JdbcLearningProgressReader(NamedParameterJdbcTemplate jdbc) { this.jdbc = jdbc; }

    public List<CourseActivity> startedCourses(UUID userId) {
        return jdbc.query("""
                WITH started AS (
                    SELECT course_id FROM course_study_activity WHERE user_id = :userId
                    UNION
                    SELECT tp.course_id FROM lesson_progress lp JOIN topic_progress tp
                        ON tp.user_id = lp.user_id AND tp.topic_id = lp.topic_id
                        WHERE lp.user_id = :userId AND tp.course_id IS NOT NULL
                )
                SELECT s.course_id, a.started_at, a.last_studied_at FROM started s
                LEFT JOIN course_study_activity a ON a.course_id = s.course_id AND a.user_id = :userId
                ORDER BY a.last_studied_at DESC NULLS LAST, s.course_id LIMIT 101
                """, Map.of("userId", userId), (rs, i) -> new CourseActivity(rs.getObject("course_id", UUID.class),
                rs.getTimestamp("started_at") == null ? null : rs.getTimestamp("started_at").toInstant(),
                rs.getTimestamp("last_studied_at") == null ? null : rs.getTimestamp("last_studied_at").toInstant()));
    }

    public Set<UUID> completedLessons(UUID userId, List<UUID> lessonIds) {
        if (lessonIds.isEmpty()) return Set.of();
        return new HashSet<>(jdbc.query("""
                SELECT lesson_id FROM lesson_progress WHERE user_id = :userId
                    AND completed_at IS NOT NULL AND lesson_id IN (:lessonIds)
                """, Map.of("userId", userId, "lessonIds", lessonIds), (rs, i) -> rs.getObject("lesson_id", UUID.class)));
    }
}
