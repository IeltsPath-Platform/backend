package com.ieltspath.content.infrastructure.persistence.adapter;

import com.ieltspath.content.application.port.SuggestionCandidateRepository;
import com.ieltspath.content.application.result.SuggestionCandidatesResult;
import com.ieltspath.content.domain.vo.PackageType;
import com.ieltspath.content.domain.vo.Skill;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public class JdbcSuggestionCandidateRepository implements SuggestionCandidateRepository {
    private final NamedParameterJdbcTemplate jdbc;

    public JdbcSuggestionCandidateRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<SuggestionCandidatesResult.PackageCandidate> packages(Collection<UUID> knowledgePointIds,
                                                                       BigDecimal maxBand) {
        return jdbc.query("""
                WITH matching_packages AS (
                    SELECT DISTINCT p.id
                    FROM question_knowledge_points qkp
                    JOIN knowledge_points kp ON kp.id = qkp.knowledge_point_id AND kp.status = 'ACTIVE'
                    JOIN topics kt ON kt.id = kp.topic_id AND kt.status = 'ACTIVE'
                    JOIN courses kc ON kc.id = kt.course_id AND kc.status = 'ACTIVE'
                    JOIN question_versions qv ON qv.id = qkp.question_version_id AND qv.status = 'PUBLISHED'
                    JOIN section_questions sq ON sq.question_version_id = qv.id
                    JOIN content_sections s ON s.id = sq.section_id
                    JOIN content_package_versions v ON v.id = s.package_version_id AND v.status = 'PUBLISHED'
                    JOIN content_packages p ON p.id = v.package_id AND p.current_published_version_id = v.id
                    WHERE qkp.knowledge_point_id IN (:knowledgePointIds)
                      AND p.package_type IN ('PRACTICE_SET', 'PRACTICE_TEST') AND p.status = 'PUBLISHED'
                )
                SELECT p.id AS package_id, v.id AS package_version_id, p.package_type, p.title,
                       targets.knowledge_point_ids,
                       GREATEST(pc.band_level, lc.band_level, tc.band_level, targets.target_band) AS band,
                       targets.skill
                FROM matching_packages matched
                JOIN content_packages p ON p.id = matched.id
                JOIN content_package_versions v ON v.id = p.current_published_version_id
                    AND v.package_id = p.id AND v.status = 'PUBLISHED'
                LEFT JOIN courses pc ON pc.id = p.course_id AND pc.status = 'ACTIVE'
                LEFT JOIN lessons l ON l.id = p.lesson_id AND l.status = 'PUBLISHED'
                LEFT JOIN topics lt ON lt.id = l.topic_id AND lt.status = 'ACTIVE'
                LEFT JOIN courses lc ON lc.id = lt.course_id AND lc.status = 'ACTIVE'
                LEFT JOIN topics pt ON pt.id = p.topic_id AND pt.status = 'ACTIVE'
                LEFT JOIN courses tc ON tc.id = pt.course_id AND tc.status = 'ACTIVE'
                JOIN LATERAL (
                    SELECT ARRAY_AGG(DISTINCT kp.id ORDER BY kp.id) AS knowledge_point_ids,
                           MAX(kc.band_level) AS target_band,
                           CASE WHEN COUNT(DISTINCT NULLIF(kp.skill, 'ALL')) = 1
                                THEN MIN(NULLIF(kp.skill, 'ALL')) END AS skill
                    FROM content_sections s
                    JOIN section_questions sq ON sq.section_id = s.id
                    JOIN question_versions qv ON qv.id = sq.question_version_id AND qv.status = 'PUBLISHED'
                    JOIN question_knowledge_points qkp ON qkp.question_version_id = qv.id
                    JOIN knowledge_points kp ON kp.id = qkp.knowledge_point_id AND kp.status = 'ACTIVE'
                    JOIN topics kt ON kt.id = kp.topic_id AND kt.status = 'ACTIVE'
                    JOIN courses kc ON kc.id = kt.course_id AND kc.status = 'ACTIVE'
                    WHERE s.package_version_id = v.id
                ) targets ON targets.knowledge_point_ids IS NOT NULL
                WHERE (p.course_id IS NULL OR pc.id IS NOT NULL)
                  AND (p.lesson_id IS NULL OR lc.id IS NOT NULL)
                  AND (p.topic_id IS NULL OR tc.id IS NOT NULL)
                  AND GREATEST(pc.band_level, lc.band_level, tc.band_level, targets.target_band) <= :maxBand
                ORDER BY band, p.package_type, p.id
                """, parameters(knowledgePointIds, maxBand), (rs, row) ->
                new SuggestionCandidatesResult.PackageCandidate(uuid(rs, "package_id"),
                        uuid(rs, "package_version_id"), PackageType.valueOf(rs.getString("package_type")),
                        rs.getString("title"), uuids(rs, "knowledge_point_ids"), rs.getBigDecimal("band"),
                        skill(rs)));
    }

    @Override
    public List<SuggestionCandidatesResult.LessonCandidate> lessons(Collection<UUID> knowledgePointIds,
                                                                     BigDecimal maxBand) {
        return jdbc.query("""
                SELECT l.id AS lesson_id, l.topic_id, l.title, taught.knowledge_point_ids,
                       c.band_level AS band, taught.skill, l.sort_order
                FROM lessons l
                JOIN topics t ON t.id = l.topic_id AND t.status = 'ACTIVE'
                JOIN courses c ON c.id = t.course_id AND c.status = 'ACTIVE'
                JOIN LATERAL (
                    SELECT ARRAY_AGG(DISTINCT kp.id ORDER BY kp.id) AS knowledge_point_ids,
                           CASE WHEN COUNT(DISTINCT NULLIF(kp.skill, 'ALL')) = 1
                                THEN MIN(NULLIF(kp.skill, 'ALL')) END AS skill
                    FROM lesson_knowledge_points lkp
                    JOIN knowledge_points kp ON kp.id = lkp.knowledge_point_id AND kp.status = 'ACTIVE'
                    JOIN topics kt ON kt.id = kp.topic_id AND kt.status = 'ACTIVE'
                    JOIN courses kc ON kc.id = kt.course_id AND kc.status = 'ACTIVE'
                    WHERE lkp.lesson_id = l.id
                ) taught ON taught.knowledge_point_ids IS NOT NULL
                WHERE l.status = 'PUBLISHED' AND c.band_level <= :maxBand
                  AND EXISTS (SELECT 1 FROM lesson_knowledge_points lkp
                              JOIN knowledge_points kp ON kp.id = lkp.knowledge_point_id AND kp.status = 'ACTIVE'
                              JOIN topics kt ON kt.id = kp.topic_id AND kt.status = 'ACTIVE'
                              JOIN courses kc ON kc.id = kt.course_id AND kc.status = 'ACTIVE'
                              WHERE lkp.lesson_id = l.id AND lkp.knowledge_point_id IN (:knowledgePointIds))
                ORDER BY c.band_level, t.sort_order, l.sort_order, l.id
                """, parameters(knowledgePointIds, maxBand), (rs, row) ->
                new SuggestionCandidatesResult.LessonCandidate(uuid(rs, "lesson_id"), uuid(rs, "topic_id"),
                        rs.getString("title"), uuids(rs, "knowledge_point_ids"), rs.getBigDecimal("band"),
                        skill(rs), rs.getInt("sort_order")));
    }

    private static MapSqlParameterSource parameters(Collection<UUID> knowledgePointIds, BigDecimal maxBand) {
        return new MapSqlParameterSource().addValue("knowledgePointIds", knowledgePointIds).addValue("maxBand", maxBand);
    }

    private static UUID uuid(ResultSet rs, String column) throws SQLException {
        return rs.getObject(column, UUID.class);
    }

    private static List<UUID> uuids(ResultSet rs, String column) throws SQLException {
        return Arrays.stream((Object[]) rs.getArray(column).getArray()).map(value -> (UUID) value).toList();
    }

    private static Skill skill(ResultSet rs) throws SQLException {
        String value = rs.getString("skill");
        return value == null ? null : Skill.valueOf(value);
    }
}
