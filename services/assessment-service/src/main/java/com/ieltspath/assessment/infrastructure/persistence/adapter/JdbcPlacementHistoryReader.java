package com.ieltspath.assessment.infrastructure.persistence.adapter;

import com.ieltspath.assessment.application.port.PlacementHistoryReader;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Component
public class JdbcPlacementHistoryReader implements PlacementHistoryReader {
    private final NamedParameterJdbcTemplate jdbc;

    public JdbcPlacementHistoryReader(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<Item> find(UUID userId, long offset, int limit) {
        var params = new MapSqlParameterSource().addValue("userId", userId)
                .addValue("offset", offset).addValue("limit", limit);
        return jdbc.query("""
                SELECT a.id, a.placement_pool, a.submitted_at, latest.id AS result_id, latest.overall_band,
                       latest.partial
                FROM assessment_attempts a
                LEFT JOIN LATERAL (
                    SELECT r.id, r.overall_band, r.partial
                    FROM assessment_results r
                    WHERE r.attempt_id = a.id AND r.status = 'COMPLETED'
                    ORDER BY r.result_version DESC
                    LIMIT 1
                ) latest ON TRUE
                WHERE a.user_id = :userId AND a.attempt_type = 'PLACEMENT' AND a.status = 'SUBMITTED'
                ORDER BY a.submitted_at DESC, a.id DESC
                LIMIT :limit OFFSET :offset
                """, params, (rs, row) -> {
            BigDecimal band = rs.getBigDecimal("overall_band");
            return new Item(rs.getObject("id", UUID.class), rs.getString("placement_pool"),
                    rs.getTimestamp("submitted_at").toInstant(), rs.getObject("result_id") != null,
                    band == null ? null : band.doubleValue(), rs.getBoolean("partial"));
        });
    }
}
