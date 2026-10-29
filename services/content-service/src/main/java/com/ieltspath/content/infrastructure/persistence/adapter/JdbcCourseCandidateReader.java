package com.ieltspath.content.infrastructure.persistence.adapter;

import com.ieltspath.content.application.port.CourseCandidateReader;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Component
public class JdbcCourseCandidateReader implements CourseCandidateReader {

    private final NamedParameterJdbcTemplate jdbc;

    public JdbcCourseCandidateReader(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<Point> pool(String cefrLevel, Set<String> skills) {
        if (cefrLevel == null) return List.of();
        MapSqlParameterSource params = new MapSqlParameterSource("level", cefrLevel);
        String skillFilter = "";
        if (!skills.isEmpty()) {
            skillFilter = " OR skill IN (:skills)";
            params.addValue("skills", List.copyOf(skills));
        }
        return jdbc.query("""
                SELECT id, code, name, description, cefr_level, skill FROM knowledge_points
                WHERE status = 'ACTIVE' AND cefr_level = :level AND (skill IS NULL OR skill = 'ALL'""" + skillFilter
                + ") ORDER BY name, code", params, JdbcCourseCandidateReader::point);
    }

    @Override
    public List<Point> activeByIds(Collection<UUID> ids) {
        if (ids.isEmpty()) return List.of();
        return jdbc.query("SELECT id, code, name, description, cefr_level, skill FROM knowledge_points "
                + "WHERE status = 'ACTIVE' AND id IN (:ids) ORDER BY name, code",
                Map.of("ids", List.copyOf(ids)), JdbcCourseCandidateReader::point);
    }

    @Override
    public Map<UUID, Set<UUID>> prerequisites(Collection<UUID> ids) {
        Map<UUID, Set<UUID>> result = new LinkedHashMap<>();
        if (ids.isEmpty()) return result;
        jdbc.query("""
                SELECT p.knowledge_point_id, p.prerequisite_id
                FROM knowledge_point_prerequisites p
                JOIN knowledge_points kp ON kp.id = p.knowledge_point_id AND kp.status = 'ACTIVE'
                JOIN knowledge_points pre ON pre.id = p.prerequisite_id AND pre.status = 'ACTIVE'
                WHERE p.knowledge_point_id IN (:ids)
                ORDER BY p.knowledge_point_id, p.prerequisite_id
                """, Map.of("ids", List.copyOf(ids)), rs -> {
            result.computeIfAbsent(uuid(rs, "knowledge_point_id"), ignored -> new LinkedHashSet<>())
                    .add(uuid(rs, "prerequisite_id"));
        });
        return result;
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

    @Override
    public Map<UUID, List<UUID>> exerciseQuestions(Collection<UUID> pointIds, int perPoint) {
        Map<UUID, List<UUID>> result = new LinkedHashMap<>();
        if (pointIds.isEmpty()) return result;
        jdbc.query("""
                SELECT point, version_id FROM (
                  SELECT qkp.knowledge_point_id AS point, qv.id AS version_id,
                         row_number() OVER (PARTITION BY qkp.knowledge_point_id ORDER BY qv.created_at, qv.id) AS rank_in_point
                  FROM question_knowledge_points qkp
                  JOIN question_versions qv ON qv.id = qkp.question_version_id
                  JOIN questions q ON q.id = qv.question_id
                  WHERE qkp.knowledge_point_id IN (:ids) AND qv.status = 'PUBLISHED' AND q.purpose = 'LEARNING'
                    -- A question has one home: one already in a lesson's exercise block or in a package section
                    -- cannot be the exercise of another lesson, so the model is never offered it.
                    AND NOT EXISTS (SELECT 1 FROM lesson_block_questions owned WHERE owned.question_version_id = qv.id)
                    AND NOT EXISTS (SELECT 1 FROM section_questions owned_sq WHERE owned_sq.question_version_id = qv.id)) ranked
                WHERE rank_in_point <= :limit
                ORDER BY point, rank_in_point
                """, new MapSqlParameterSource("ids", List.copyOf(pointIds)).addValue("limit", perPoint), rs -> {
            result.computeIfAbsent(uuid(rs, "point"), ignored -> new ArrayList<>()).add(uuid(rs, "version_id"));
        });
        return result;
    }

    @Override
    public int highestRootTopicSortOrder() {
        Integer highest = jdbc.getJdbcOperations().queryForObject(
                "SELECT COALESCE(MAX(sort_order), 0) FROM topics WHERE parent_topic_id IS NULL", Integer.class);
        return highest == null ? 0 : highest;
    }

    private static Point point(ResultSet rs, int row) throws SQLException {
        return new Point(uuid(rs, "id"), rs.getString("code"), rs.getString("name"), rs.getString("description"),
                rs.getString("cefr_level"), rs.getString("skill"));
    }

    private static UUID uuid(ResultSet rs, String column) throws SQLException {
        return rs.getObject(column, UUID.class);
    }
}
