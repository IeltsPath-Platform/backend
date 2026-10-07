package com.group01.assessment.infrastructure.persistence.adapter;

import com.group01.assessment.application.port.AssessmentLlmQuota;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.sql.Date;
import java.util.UUID;

/** Atomically reserves one provider call against a learner's daily quota. */
@Repository
public class JdbcAssessmentLlmQuota implements AssessmentLlmQuota {
    private final NamedParameterJdbcTemplate jdbc;

    public JdbcAssessmentLlmQuota(NamedParameterJdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    public boolean tryConsume(UUID userId, LocalDate day, int limit) {
        if (limit <= 0) return false;
        String sql = """
                INSERT INTO llm_usage_daily (user_id, usage_date, count)
                VALUES (:userId, :day, 1)
                ON CONFLICT (user_id, usage_date) DO UPDATE
                SET count = llm_usage_daily.count + 1
                WHERE llm_usage_daily.count < :limit
                RETURNING count
                """;
        return !jdbc.query(sql, new MapSqlParameterSource().addValue("userId", userId)
                .addValue("day", Date.valueOf(day)).addValue("limit", limit), (rs, rowNum) -> rs.getInt(1)).isEmpty();
    }
}
