package com.ieltspath.learning.infrastructure.persistence;

import com.ieltspath.learning.application.port.LearningOverviewReader;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public class JdbcLearningOverviewReader implements LearningOverviewReader {
    private final JdbcTemplate jdbc;

    public JdbcLearningOverviewReader(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Totals totals(UUID userId) {
        return jdbc.queryForObject("""
                SELECT (SELECT started_at FROM study_tracking_policy WHERE id = 1) AS data_from,
                       coalesce(sum(active_ms), 0) AS study_ms,
                       (SELECT count(DISTINCT lesson_id) FROM lesson_progress
                        WHERE user_id = ? AND completed_at IS NOT NULL) AS completed_lessons
                FROM study_daily_totals
                WHERE user_id = ? AND scope_key = 'ALL'
                """, (rs, row) -> new Totals(rs.getTimestamp("data_from").toInstant(),
                rs.getLong("study_ms"), rs.getLong("completed_lessons")),
                userId, userId);
    }
}
