package com.ieltspath.learning.infrastructure.persistence;

import com.ieltspath.learning.domain.aggregate.CourseProgress;
import com.ieltspath.learning.domain.repository.CourseProgressRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Repository
public class JdbcCourseProgressRepository implements CourseProgressRepository {
    private final JdbcTemplate jdbc;

    public JdbcCourseProgressRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    public Map<UUID, CourseProgress> findAll(UUID userId) {
        Map<UUID, CourseProgress> progress = new LinkedHashMap<>();
        jdbc.query("SELECT user_id, course_id, passed_at FROM course_progress WHERE user_id = ? ORDER BY course_id",
                row -> {
                    UUID courseId = row.getObject("course_id", UUID.class);
                    progress.put(courseId, CourseProgress.restore(row.getObject("user_id", UUID.class), courseId,
                            row.getTimestamp("passed_at").toInstant()));
                }, userId);
        return Map.copyOf(progress);
    }

    @Override
    public void save(CourseProgress progress) {
        jdbc.update("""
                INSERT INTO course_progress (user_id, course_id, passed_at) VALUES (?, ?, ?)
                ON CONFLICT (user_id, course_id) DO UPDATE SET passed_at = course_progress.passed_at
                """, progress.userId(), progress.courseId(), Timestamp.from(progress.passedAt()));
    }
}
