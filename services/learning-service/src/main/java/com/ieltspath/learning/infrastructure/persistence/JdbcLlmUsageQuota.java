package com.ieltspath.learning.infrastructure.persistence;

import com.ieltspath.learning.application.port.LlmUsageQuota;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.UUID;

/** One conditional upsert, atomic without the learner's lock. */
@Repository
public class JdbcLlmUsageQuota implements LlmUsageQuota {
    private final JdbcTemplate jdbc;

    public JdbcLlmUsageQuota(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean tryConsume(UUID userId, LocalDate day, String kind, int limit) {
        return jdbc.update("""
                INSERT INTO llm_daily_usage (user_id, usage_date, kind, count) VALUES (?, ?, ?, 1)
                ON CONFLICT (user_id, usage_date, kind) DO UPDATE SET count = llm_daily_usage.count + 1
                WHERE llm_daily_usage.count < ?
                """, userId, day, kind, limit) == 1;
    }
}
