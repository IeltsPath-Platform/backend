package com.ieltspath.assessment.infrastructure.persistence.adapter;

import com.ieltspath.assessment.application.port.PracticeAttemptReader;
import com.ieltspath.assessment.domain.vo.AttemptScope;
import com.ieltspath.assessment.domain.vo.Skill;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class JdbcPracticeAttemptReader implements PracticeAttemptReader {
    private final NamedParameterJdbcTemplate jdbc;

    public JdbcPracticeAttemptReader(NamedParameterJdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    public List<ProgressKey> findProgress(UUID userId, Skill skill, AttemptScope scope, List<UUID> packageIds) {
        String sql = scope == AttemptScope.FULL ? """
                SELECT DISTINCT a.package_id, NULL::integer AS part_no
                FROM assessment_attempts a
                WHERE a.user_id = :userId AND a.attempt_type = 'OFFICIAL_PRACTICE'
                  AND a.skill = :skill AND a.scope = 'FULL' AND a.status = 'SUBMITTED'
                  AND (:allPackages OR a.package_id = ANY (CAST(:packageIds AS uuid[])))
                ORDER BY a.package_id
                """ : """
                SELECT completed.package_id, completed.part_no
                FROM (
                    SELECT a.package_id, s.part_no
                    FROM assessment_attempts a JOIN attempt_sections s ON s.attempt_id = a.id
                    WHERE a.user_id = :userId AND a.attempt_type = 'OFFICIAL_PRACTICE'
                      AND a.skill = :skill AND a.status = 'SUBMITTED' AND a.scope = 'PART' AND s.part_no IS NOT NULL
                      AND (:allPackages OR a.package_id = ANY (CAST(:packageIds AS uuid[])))
                    GROUP BY a.id, a.package_id, s.part_no
                    UNION
                    SELECT a.package_id, s.part_no
                    FROM assessment_attempts a JOIN attempt_sections s ON s.attempt_id = a.id
                    WHERE a.user_id = :userId AND a.attempt_type = 'OFFICIAL_PRACTICE'
                      AND a.skill = :skill AND a.status = 'SUBMITTED' AND a.scope = 'FULL' AND s.part_no IS NOT NULL
                      AND (:allPackages OR a.package_id = ANY (CAST(:packageIds AS uuid[])))
                ) completed
                ORDER BY completed.package_id, completed.part_no
                """;
        var params = new MapSqlParameterSource().addValue("userId", userId).addValue("skill", skill.name())
                .addValue("allPackages", packageIds.isEmpty())
                .addValue("packageIds", packageIds.stream().map(UUID::toString)
                        .collect(java.util.stream.Collectors.joining(",", "{", "}")));
        return jdbc.query(sql, params,
                (rs, row) -> new ProgressKey(rs.getObject("package_id", UUID.class), (Integer) rs.getObject("part_no")));
    }

    @Override
    public HistoryPage findHistory(UUID userId, Skill skill, int page, int size) {
        var params = new MapSqlParameterSource().addValue("userId", userId)
                .addValue("skill", skill == null ? null : skill.name()).addValue("limit", size)
                .addValue("offset", (long) page * size);
        String filter = "(CAST(:skill AS varchar) IS NULL OR a.skill = :skill)";
        List<HistoryItem> rows = jdbc.query("""
                SELECT a.id, a.package_id, a.package_version_id, a.skill, a.scope, a.status, a.submitted_at,
                       array_remove(array_agg(DISTINCT s.part_no ORDER BY s.part_no), NULL) AS part_nos,
                       latest.percent
                FROM assessment_attempts a
                LEFT JOIN attempt_sections s ON s.attempt_id = a.id
                LEFT JOIN LATERAL (
                    SELECT CASE WHEN SUM(ir.max_score) > 0
                                THEN ROUND(100.0 * SUM(ir.score) / SUM(ir.max_score), 2) ELSE 0 END AS percent
                    FROM assessment_results r
                    JOIN item_results ir ON ir.result_id = r.id
                    WHERE r.attempt_id = a.id AND r.status = 'COMPLETED'
                      AND r.result_version = (SELECT MAX(r2.result_version) FROM assessment_results r2
                                              WHERE r2.attempt_id = a.id AND r2.status = 'COMPLETED')
                ) latest ON TRUE
                WHERE a.user_id = :userId AND a.attempt_type = 'OFFICIAL_PRACTICE' AND a.status = 'SUBMITTED'
                  AND """ + filter + """
                GROUP BY a.id, latest.percent
                ORDER BY a.submitted_at DESC, a.id DESC
                LIMIT :limit OFFSET :offset
                """, params, (rs, row) -> historyItem(rs));
        Long total = jdbc.queryForObject("""
                SELECT COUNT(*) FROM assessment_attempts a
                WHERE a.user_id = :userId AND a.attempt_type = 'OFFICIAL_PRACTICE' AND a.status = 'SUBMITTED'
                  AND """ + filter, params, Long.class);
        return new HistoryPage(total == null ? 0 : total, rows);
    }

    private static HistoryItem historyItem(ResultSet rs) throws SQLException {
        Array array = rs.getArray("part_nos");
        List<Integer> parts = new ArrayList<>();
        if (array != null) {
            Object[] values = (Object[]) array.getArray();
            for (Object value : values) parts.add(((Number) value).intValue());
        }
        return new HistoryItem(rs.getObject("id", UUID.class), rs.getObject("package_id", UUID.class),
                rs.getObject("package_version_id", UUID.class), rs.getString("skill") == null ? null
                : Skill.valueOf(rs.getString("skill")), AttemptScope.valueOf(rs.getString("scope")), parts,
                rs.getString("status"), rs.getTimestamp("submitted_at") == null ? null
                : rs.getTimestamp("submitted_at").toInstant(), rs.getObject("percent") == null ? null
                : rs.getBigDecimal("percent").doubleValue());
    }
}
