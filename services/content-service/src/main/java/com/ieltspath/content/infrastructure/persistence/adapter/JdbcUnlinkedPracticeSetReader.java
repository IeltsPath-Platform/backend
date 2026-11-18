package com.ieltspath.content.infrastructure.persistence.adapter;

import com.ieltspath.content.application.port.UnlinkedPracticeSetReader;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class JdbcUnlinkedPracticeSetReader implements UnlinkedPracticeSetReader {

    private final NamedParameterJdbcTemplate jdbc;

    public JdbcUnlinkedPracticeSetReader(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Map<UUID, List<PracticeSetRef>> practiceSets(Collection<UUID> pointIds, int perPoint) {
        Map<UUID, List<PracticeSetRef>> result = new LinkedHashMap<>();
        if (pointIds.isEmpty()) return result;
        jdbc.query("""
                SELECT point, package_id, code FROM (
                  SELECT qkp.knowledge_point_id AS point, p.id AS package_id, p.code,
                         row_number() OVER (PARTITION BY qkp.knowledge_point_id ORDER BY p.code, p.id) AS rank_in_point
                  FROM content_packages p
                  JOIN content_sections s ON s.package_version_id = p.current_published_version_id
                  JOIN section_questions sq ON sq.section_id = s.id
                  JOIN question_knowledge_points qkp ON qkp.question_version_id = sq.question_version_id
                  WHERE p.package_type = 'PRACTICE_SET' AND p.status = 'PUBLISHED' AND p.lesson_id IS NULL
                    AND qkp.knowledge_point_id IN (:ids)
                  GROUP BY qkp.knowledge_point_id, p.id, p.code) ranked
                WHERE rank_in_point <= :limit
                ORDER BY point, rank_in_point
                """, new MapSqlParameterSource("ids", List.copyOf(pointIds)).addValue("limit", perPoint), rs -> {
            result.computeIfAbsent(uuid(rs, "point"), ignored -> new ArrayList<>())
                    .add(new PracticeSetRef(uuid(rs, "package_id"), rs.getString("code")));
        });
        return result;
    }

    private static UUID uuid(ResultSet rs, String column) throws SQLException {
        return rs.getObject(column, UUID.class);
    }
}
