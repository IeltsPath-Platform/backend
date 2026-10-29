package com.ieltspath.content.infrastructure.persistence.adapter;

import com.ieltspath.content.application.port.CourseCatalogReader;
import com.ieltspath.content.domain.vo.CourseCatalogFacts;
import com.ieltspath.content.domain.vo.CourseCatalogFacts.KnowledgePointFact;
import com.ieltspath.content.domain.vo.CourseCatalogFacts.PackageFact;
import com.ieltspath.content.domain.vo.CourseCatalogFacts.Query;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Reads the catalogue facts a course draft is checked against: a handful of queries, whatever the draft's size. */
@Component
public class JdbcCourseCatalogReader implements CourseCatalogReader {

    private final NamedParameterJdbcTemplate jdbc;

    public JdbcCourseCatalogReader(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public CourseCatalogFacts read(Query query) {
        Map<UUID, KnowledgePointFact> points = knowledgePoints(query.knowledgePointIds());
        Set<UUID> packageIds = new LinkedHashSet<>(query.practiceSetPackageIds());
        packageIds.addAll(query.milestonePackageIds());
        Map<String, UUID> questionRefs = resolveQuestionRefs(query.questionRefs());
        return new CourseCatalogFacts(
                points,
                expectedKnowledgePoints(query.cefrLevel(), query.skills()),
                prerequisites(query.knowledgePointIds()),
                lowestBandTaught(query.knowledgePointIds()),
                packages(packageIds),
                milestonePackagesLinked(query.milestonePackageIds()),
                unlinkedPracticeSets(query.knowledgePointIds()),
                withoutExerciseQuestion(query.knowledgePointIds()),
                questionRefs,
                existing("courses", query.courseCodes()),
                existing("topics", query.topicCodes()),
                existing("lessons", query.lessonCodes()),
                existing("content_packages", query.packageCodes()),
                query.band() != null && Boolean.TRUE.equals(jdbc.queryForObject(
                        "SELECT EXISTS (SELECT 1 FROM courses WHERE band_level = :band)",
                        Map.of("band", query.band()), Boolean.class)),
                new HashSet<>(jdbc.query("SELECT sort_order FROM topics WHERE parent_topic_id IS NULL",
                        (rs, i) -> rs.getInt(1))),
                ownedQuestionVersions(questionRefs.values()));
    }

    private Map<UUID, KnowledgePointFact> knowledgePoints(Collection<UUID> ids) {
        Map<UUID, KnowledgePointFact> points = new LinkedHashMap<>();
        if (ids.isEmpty()) return points;
        jdbc.query("SELECT id, code, status FROM knowledge_points WHERE id IN (:ids)", Map.of("ids", ids), rs -> {
            UUID id = uuid(rs, "id");
            points.put(id, new KnowledgePointFact(id, rs.getString("code"), rs.getString("status")));
        });
        return points;
    }

    /** Active points of the course's CEFR level whose skill is one of the course's, or no skill / ALL. */
    private Set<UUID> expectedKnowledgePoints(String cefrLevel, Set<String> skills) {
        if (cefrLevel == null) return Set.of();
        MapSqlParameterSource params = new MapSqlParameterSource("level", cefrLevel);
        String skillFilter = "";
        if (!skills.isEmpty()) {
            skillFilter = " OR skill IN (:skills)";
            params.addValue("skills", List.copyOf(skills));
        }
        return new LinkedHashSet<>(jdbc.query("""
                SELECT id FROM knowledge_points
                WHERE status = 'ACTIVE' AND cefr_level = :level AND (skill IS NULL OR skill = 'ALL'""" + skillFilter + ")",
                params, (rs, i) -> uuid(rs, "id")));
    }

    private Map<UUID, Set<UUID>> prerequisites(Collection<UUID> ids) {
        Map<UUID, Set<UUID>> result = new HashMap<>();
        if (ids.isEmpty()) return result;
        jdbc.query("""
                SELECT p.knowledge_point_id, p.prerequisite_id
                FROM knowledge_point_prerequisites p
                JOIN knowledge_points kp ON kp.id = p.knowledge_point_id AND kp.status = 'ACTIVE'
                JOIN knowledge_points pre ON pre.id = p.prerequisite_id AND pre.status = 'ACTIVE'
                WHERE p.knowledge_point_id IN (:ids)
                """, Map.of("ids", ids), rs -> {
            result.computeIfAbsent(uuid(rs, "knowledge_point_id"), ignored -> new LinkedHashSet<>())
                    .add(uuid(rs, "prerequisite_id"));
        });
        return result;
    }

    /** The lowest band of an existing course that has a lesson teaching each point. */
    private Map<UUID, BigDecimal> lowestBandTaught(Collection<UUID> ids) {
        Map<UUID, BigDecimal> result = new HashMap<>();
        if (ids.isEmpty()) return result;
        jdbc.query("""
                SELECT lkp.knowledge_point_id, min(c.band_level) AS band
                FROM lesson_knowledge_points lkp
                JOIN lessons l ON l.id = lkp.lesson_id
                JOIN topics t ON t.id = l.topic_id
                JOIN courses c ON c.id = t.course_id
                WHERE lkp.knowledge_point_id IN (:ids)
                GROUP BY lkp.knowledge_point_id
                """, Map.of("ids", ids), rs -> {
            result.put(uuid(rs, "knowledge_point_id"), rs.getBigDecimal("band"));
        });
        return result;
    }

    private Map<UUID, PackageFact> packages(Collection<UUID> ids) {
        Map<UUID, PackageFact> result = new LinkedHashMap<>();
        if (ids.isEmpty()) return result;
        jdbc.query("SELECT id, code, package_type, status, lesson_id, topic_id, course_id "
                + "FROM content_packages WHERE id IN (:ids)", Map.of("ids", ids), rs -> {
            UUID id = uuid(rs, "id");
            result.put(id, new PackageFact(id, rs.getString("code"), rs.getString("package_type"),
                    rs.getString("status"), uuid(rs, "lesson_id"), uuid(rs, "topic_id"), uuid(rs, "course_id")));
        });
        return result;
    }

    private Set<UUID> milestonePackagesLinked(Collection<UUID> ids) {
        if (ids.isEmpty()) return Set.of();
        return new HashSet<>(jdbc.query("SELECT package_id FROM course_practice_test_milestones "
                + "WHERE package_id IN (:ids)", Map.of("ids", ids), (rs, i) -> uuid(rs, "package_id")));
    }

    /** Published practice sets that belong to no lesson, counted per point they measure. */
    private Map<UUID, Integer> unlinkedPracticeSets(Collection<UUID> ids) {
        Map<UUID, Integer> result = new HashMap<>();
        if (ids.isEmpty()) return result;
        jdbc.query("""
                SELECT qkp.knowledge_point_id, count(DISTINCT p.id) AS sets
                FROM content_packages p
                JOIN content_sections s ON s.package_version_id = p.current_published_version_id
                JOIN section_questions sq ON sq.section_id = s.id
                JOIN question_knowledge_points qkp ON qkp.question_version_id = sq.question_version_id
                WHERE p.package_type = 'PRACTICE_SET' AND p.status = 'PUBLISHED' AND p.lesson_id IS NULL
                  AND qkp.knowledge_point_id IN (:ids)
                GROUP BY qkp.knowledge_point_id
                """, Map.of("ids", ids), rs -> {
            result.put(uuid(rs, "knowledge_point_id"), rs.getInt("sets"));
        });
        return result;
    }

    /** The given points that no published learning question measures. */
    private Set<UUID> withoutExerciseQuestion(Collection<UUID> ids) {
        Set<UUID> without = new LinkedHashSet<>(ids);
        if (ids.isEmpty()) return without;
        jdbc.query("""
                SELECT DISTINCT qkp.knowledge_point_id
                FROM question_knowledge_points qkp
                JOIN question_versions qv ON qv.id = qkp.question_version_id
                JOIN questions q ON q.id = qv.question_id
                WHERE qkp.knowledge_point_id IN (:ids) AND qv.status = 'PUBLISHED' AND q.purpose = 'LEARNING'
                  AND NOT EXISTS (SELECT 1 FROM lesson_block_questions owned WHERE owned.question_version_id = qv.id)
                  AND NOT EXISTS (SELECT 1 FROM section_questions owned_sq WHERE owned_sq.question_version_id = qv.id)
                """, Map.of("ids", ids), rs -> {
            without.remove(uuid(rs, "knowledge_point_id"));
        });
        return without;
    }

    /**
     * The question versions that already belong to a lesson's exercise block or to a package section. A question has
     * one home, so such a version cannot also be the exercise of a new lesson.
     */
    private Set<UUID> ownedQuestionVersions(Collection<UUID> versionIds) {
        Set<UUID> owned = new HashSet<>();
        if (versionIds.isEmpty()) return owned;
        jdbc.query("""
                SELECT question_version_id FROM lesson_block_questions WHERE question_version_id IN (:ids)
                UNION
                SELECT question_version_id FROM section_questions WHERE question_version_id IN (:ids)
                """, Map.of("ids", List.copyOf(versionIds)), rs -> {
            owned.add(uuid(rs, "question_version_id"));
        });
        return owned;
    }

    /**
     * A reference is a question version id or the import reference of a question, whose latest version is used.
     * Anything that matches neither is left out.
     */
    private Map<String, UUID> resolveQuestionRefs(Collection<String> refs) {
        Map<String, UUID> resolved = new HashMap<>();
        if (refs.isEmpty()) return resolved;
        List<UUID> versionIds = new java.util.ArrayList<>();
        for (String ref : refs) {
            try {
                versionIds.add(UUID.fromString(ref));
            } catch (IllegalArgumentException notAnId) {
                // an import reference
            }
        }
        if (!versionIds.isEmpty()) {
            jdbc.query("SELECT id FROM question_versions WHERE id IN (:ids)", Map.of("ids", versionIds), rs -> {
                resolved.put(uuid(rs, "id").toString(), uuid(rs, "id"));
            });
        }
        jdbc.query("""
                SELECT q.import_ref,
                       (SELECT qv.id FROM question_versions qv WHERE qv.question_id = q.id
                        ORDER BY qv.version_number DESC LIMIT 1) AS version_id
                FROM questions q WHERE q.import_ref IN (:refs)
                """, Map.of("refs", List.copyOf(refs)), rs -> {
            UUID version = uuid(rs, "version_id");
            if (version != null) resolved.put(rs.getString("import_ref"), version);
        });
        return resolved;
    }

    /** The given codes that already exist in {@code table}. The table name is fixed by the callers above. */
    private Set<String> existing(String table, Collection<String> codes) {
        if (codes.isEmpty()) return Set.of();
        return new HashSet<>(jdbc.query("SELECT code FROM " + table + " WHERE code IN (:codes)",
                Map.of("codes", List.copyOf(codes)), (rs, i) -> rs.getString("code")));
    }

    private static UUID uuid(ResultSet rs, String column) throws SQLException {
        return rs.getObject(column, UUID.class);
    }
}
