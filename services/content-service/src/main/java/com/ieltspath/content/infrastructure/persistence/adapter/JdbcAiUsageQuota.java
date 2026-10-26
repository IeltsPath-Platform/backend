package com.ieltspath.content.infrastructure.persistence.adapter;

import com.ieltspath.content.application.port.AiUsageQuota;
import com.ieltspath.content.domain.vo.AiUsageKind;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.UUID;

@Component
public class JdbcAiUsageQuota implements AiUsageQuota {

    private final NamedParameterJdbcTemplate jdbc;

    public JdbcAiUsageQuota(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * One statement decides: it inserts the first use of the day, or adds one only while the count is below the limit.
     * No row is returned when the limit is already reached.
     */
    @Override
    public boolean tryConsume(UUID userId, LocalDate day, AiUsageKind kind, int dailyLimit) {
        int changed = jdbc.update("""
                INSERT INTO ai_usage_daily (user_id, usage_date, kind, count)
                VALUES (:userId, :day, :kind, 1)
                ON CONFLICT (user_id, usage_date, kind)
                DO UPDATE SET count = ai_usage_daily.count + 1
                WHERE ai_usage_daily.count < :limit
                """, params(userId, day, kind).addValue("limit", dailyLimit));
        return changed == 1;
    }

    @Override
    public void refund(UUID userId, LocalDate day, AiUsageKind kind) {
        jdbc.update("""
                UPDATE ai_usage_daily SET count = count - 1
                WHERE user_id = :userId AND usage_date = :day AND kind = :kind AND count > 0
                """, params(userId, day, kind));
    }

    private static MapSqlParameterSource params(UUID userId, LocalDate day, AiUsageKind kind) {
        return new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("day", day)
                .addValue("kind", kind.name());
    }
}
