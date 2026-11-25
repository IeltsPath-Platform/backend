package com.ieltspath.assessment.infrastructure.persistence.adapter;

import com.ieltspath.assessment.application.port.PackageAttemptStatsReader;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public class JdbcPackageAttemptStatsReader implements PackageAttemptStatsReader {
    private final NamedParameterJdbcTemplate jdbc;

    public JdbcPackageAttemptStatsReader(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<PackageStats> forPackages(Collection<UUID> packageIds) {
        if (packageIds == null || packageIds.isEmpty()) return List.of();
        return jdbc.query("""
                SELECT a.package_id,
                       COUNT(*) AS started,
                       COUNT(*) FILTER (WHERE a.status = 'SUBMITTED') AS submitted,
                       AVG(latest.overall_band) AS average_band
                FROM assessment_attempts a
                LEFT JOIN LATERAL (SELECT r.overall_band FROM assessment_results r
                                   WHERE r.attempt_id = a.id AND r.status = 'COMPLETED' AND r.overall_band IS NOT NULL
                                   ORDER BY r.result_version DESC LIMIT 1) latest ON TRUE
                WHERE a.package_id IN (:ids)
                GROUP BY a.package_id
                ORDER BY a.package_id
                """, new MapSqlParameterSource("ids", packageIds),
                (rs, i) -> new PackageStats(rs.getObject("package_id", UUID.class), rs.getLong("started"),
                        rs.getLong("submitted"), rs.getBigDecimal("average_band") == null ? null
                        : rs.getBigDecimal("average_band").setScale(1, java.math.RoundingMode.HALF_UP)));
    }
}
