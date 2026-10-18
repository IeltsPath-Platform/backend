package com.ieltspath.learning.infrastructure.persistence;

import com.ieltspath.learning.domain.aggregate.CourseStudyActivity;
import com.ieltspath.learning.domain.repository.CourseStudyActivityRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.sql.Timestamp;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcCourseStudyActivityRepository implements CourseStudyActivityRepository {
    private final JdbcTemplate jdbc;
    public JdbcCourseStudyActivityRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public Optional<CourseStudyActivity> find(UUID userId, UUID courseId) {
        return jdbc.query("SELECT * FROM course_study_activity WHERE user_id = ? AND course_id = ?", (rs, i) ->
                new CourseStudyActivity(userId, courseId,
                        rs.getTimestamp("started_at") == null ? null : rs.getTimestamp("started_at").toInstant(),
                        rs.getTimestamp("last_studied_at").toInstant()), userId, courseId).stream().findFirst();
    }

    public boolean hasLegacyStudy(UUID userId, UUID courseId) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS(SELECT 1 FROM lesson_progress lp JOIN topic_progress tp
                    ON tp.user_id = lp.user_id AND tp.topic_id = lp.topic_id
                    WHERE lp.user_id = ? AND tp.course_id = ?)
                """, Boolean.class, userId, courseId));
    }

    public void save(CourseStudyActivity activity) {
        jdbc.update("""
                INSERT INTO course_study_activity(user_id, course_id, started_at, last_studied_at)
                VALUES (?, ?, ?, ?) ON CONFLICT(user_id, course_id)
                DO UPDATE SET last_studied_at = EXCLUDED.last_studied_at
                """, activity.userId(), activity.courseId(),
                activity.startedAt() == null ? null : Timestamp.from(activity.startedAt()),
                Timestamp.from(activity.lastStudiedAt()));
    }
}
