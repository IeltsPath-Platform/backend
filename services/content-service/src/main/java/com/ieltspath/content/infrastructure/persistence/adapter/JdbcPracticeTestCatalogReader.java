package com.ieltspath.content.infrastructure.persistence.adapter;

import com.ieltspath.content.application.port.PracticeTestCatalogReader;
import com.ieltspath.content.application.result.PracticeTestCardResult;
import com.ieltspath.content.domain.vo.Skill;
import com.ieltspath.content.domain.vo.TestSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

@Component
public class JdbcPracticeTestCatalogReader implements PracticeTestCatalogReader {
    private final NamedParameterJdbcTemplate jdbc;

    public JdbcPracticeTestCatalogReader(NamedParameterJdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    public Page search(Search query) {
        var params = new MapSqlParameterSource().addValue("skill", query.skill().name())
                .addValue("partNo", query.partNo()).addValue("sources", query.sources())
                .addValue("query", query.query() == null || query.query().isBlank() ? null : "%" + escape(query.query()) + "%")
                .addValue("limit", query.size()).addValue("offset", Math.multiplyExact(query.page(), query.size()));
        StringBuilder where = new StringBuilder("p.package_type = 'PRACTICE_TEST' AND p.status = 'PUBLISHED' "
                + "AND v.status = 'PUBLISHED' AND p.current_published_version_id = v.id AND s.skill = :skill ");
        if ("PART".equals(query.scope())) where.append("AND s.part_no IS NOT NULL AND (CAST(:partNo AS integer) IS NULL OR s.part_no = :partNo) ");
        if (!query.sources().isEmpty()) where.append("AND p.source_code IN (:sources) ");
        if ("FULL".equals(query.scope())) {
            where.append("AND (CAST(:query AS varchar) IS NULL OR p.title ILIKE :query ESCAPE '!' "
                    + "OR EXISTS (SELECT 1 FROM content_sections search_section "
                    + "WHERE search_section.package_version_id = v.id "
                    + "AND search_section.title ILIKE :query ESCAPE '!')) ");
        } else {
            where.append("AND (CAST(:query AS varchar) IS NULL OR p.title ILIKE :query ESCAPE '!' "
                    + "OR s.title ILIKE :query ESCAPE '!') ");
        }
        List<String> formats = query.formats() == null ? List.of() : query.formats();
        if (!formats.isEmpty()) {
            params.addValue("formats", formats);
            where.append("PART".equals(query.scope())
                    ? "AND EXISTS (SELECT 1 FROM section_question_groups fg WHERE fg.section_id = s.id AND fg.format_code IN (:formats)) "
                    : "AND EXISTS (SELECT 1 FROM section_question_groups fg JOIN content_sections fs ON fs.id = fg.section_id "
                    + "WHERE fs.package_version_id = v.id AND fg.format_code IN (:formats)) ");
        }
        addKeyFilters(query, where, params);

        String cardSql = "PART".equals(query.scope()) ? """
                SELECT p.id package_id, p.code package_code, v.id version_id, s.id section_id,
                       s.part_no, s.title, s.skill, p.title package_title, p.required_feature_key,
                       p.source_code, v.published_at, s.time_limit_seconds,
                       COUNT(DISTINCT sq.question_version_id)::int question_count,
                       (SELECT array_agg(f.format_code ORDER BY f.first_order)
                        FROM (SELECT format_code, MIN(sort_order) first_order FROM section_question_groups
                              WHERE section_id = s.id GROUP BY format_code) f) formats,
                       CASE WHEN a.asset_type = 'IMAGE' AND a.validation_status = 'VALID' THEN a.media_reference END cover_reference
                FROM content_packages p
                JOIN content_package_versions v ON v.id = p.current_published_version_id
                JOIN content_sections s ON s.package_version_id = v.id
                LEFT JOIN section_questions sq ON sq.section_id = s.id
                LEFT JOIN content_assets a ON a.id = s.cover_asset_id
                WHERE %s
                GROUP BY p.id, v.id, s.id, a.id
                ORDER BY v.published_at DESC, p.code, s.part_no
                LIMIT :limit OFFSET :offset
                """.formatted(where) : """
                SELECT p.id package_id, p.code package_code, v.id version_id, NULL::uuid section_id,
                       NULL::int part_no, p.title, s.skill, p.title package_title, p.required_feature_key,
                       p.source_code, v.published_at, CASE WHEN COUNT(*) FILTER (WHERE s.time_limit_seconds IS NULL) = 0
                           THEN SUM(s.time_limit_seconds)::int ELSE NULL END time_limit_seconds,
                       SUM(section_counts.question_count)::int question_count,
                       (SELECT array_agg(f.format_code ORDER BY f.first_order)
                        FROM (SELECT g.format_code, MIN(g.sort_order) first_order
                              FROM content_sections fs JOIN section_question_groups g ON g.section_id = fs.id
                              WHERE fs.package_version_id = v.id GROUP BY g.format_code) f) formats,
                       (array_agg(CASE WHEN a.asset_type = 'IMAGE' AND a.validation_status = 'VALID'
                                       THEN a.media_reference END ORDER BY s.part_no NULLS LAST))[1] cover_reference
                FROM content_packages p
                JOIN content_package_versions v ON v.id = p.current_published_version_id
                JOIN content_sections s ON s.package_version_id = v.id
                LEFT JOIN content_assets a ON a.id = s.cover_asset_id
                LEFT JOIN LATERAL (SELECT COUNT(*)::int question_count FROM section_questions sq WHERE sq.section_id = s.id) section_counts ON TRUE
                WHERE %s
                GROUP BY p.id, v.id, s.skill
                ORDER BY v.published_at DESC, p.code
                LIMIT :limit OFFSET :offset
                """.formatted(where);
        List<PracticeTestCardResult> cards = jdbc.query(cardSql, params, this::card);
        Long total = jdbc.queryForObject("""
                SELECT COUNT(*) FROM (
                    SELECT p.id, %s
                    FROM content_packages p
                    JOIN content_package_versions v ON v.id = p.current_published_version_id
                    JOIN content_sections s ON s.package_version_id = v.id
                    WHERE %s
                    GROUP BY %s
                ) cards
                """.formatted("PART".equals(query.scope()) ? "s.id" : "v.id", where,
                "PART".equals(query.scope()) ? "p.id, v.id, s.id" : "p.id, v.id"), params, Long.class);
        if ("FULL".equals(query.scope()) && !cards.isEmpty()) cards = addParts(cards);
        return new Page(total == null ? 0 : total, cards);
    }

    @Override
    public List<FormatCount> listFormats(Skill skill) {
        return jdbc.query("""
                SELECT g.format_code, COUNT(DISTINCT s.id) section_count
                FROM content_packages p
                JOIN content_package_versions v ON v.id = p.current_published_version_id
                JOIN content_sections s ON s.package_version_id = v.id
                JOIN section_question_groups g ON g.section_id = s.id
                WHERE p.package_type = 'PRACTICE_TEST' AND p.status = 'PUBLISHED' AND v.status = 'PUBLISHED'
                  AND s.skill = :skill
                GROUP BY g.format_code
                ORDER BY section_count DESC, g.format_code
                """, Map.of("skill", skill.name()),
                (rs, row) -> new FormatCount(rs.getString("format_code"), rs.getLong("section_count")));
    }

    private void addKeyFilters(Search query, StringBuilder where, MapSqlParameterSource params) {
        List<Key> keys = query.only() == null ? List.of() : query.only();
        boolean only = !keys.isEmpty();
        if (!only && query.exclude() != null) keys = query.exclude();
        if (keys.isEmpty()) return;
        List<String> clauses = new ArrayList<>();
        for (int i = 0; i < keys.size(); i++) {
            Key key = keys.get(i);
            String id = "keyId" + i;
            params.addValue(id, key.packageId());
            if ("PART".equals(query.scope())) {
                String part = "keyPart" + i;
                params.addValue(part, key.partNo() == null ? 0 : key.partNo());
                clauses.add("(p.id = :" + id + " AND COALESCE(s.part_no, 0) = :" + part + ")");
            } else clauses.add("p.id = :" + id);
        }
        String joined = "(" + String.join(" OR ", clauses) + ")";
        where.append(only ? "AND " + joined + " " : "AND NOT " + joined + " ");
    }

    private List<PracticeTestCardResult> addParts(List<PracticeTestCardResult> cards) {
        List<UUID> ids = cards.stream().map(PracticeTestCardResult::packageVersionId).toList();
        Map<UUID, List<PracticeTestCardResult.Part>> parts = new HashMap<>();
        jdbc.query("""
                SELECT s.package_version_id, s.id, s.part_no, s.title, COUNT(DISTINCT sq.question_version_id)::int question_count,
                       (SELECT array_agg(f.format_code ORDER BY f.first_order)
                        FROM (SELECT format_code, MIN(sort_order) first_order FROM section_question_groups
                              WHERE section_id = s.id GROUP BY format_code) f) formats
                FROM content_sections s LEFT JOIN section_questions sq ON sq.section_id = s.id
                WHERE s.package_version_id IN (:ids)
                GROUP BY s.id ORDER BY s.package_version_id, s.part_no
                """, Map.of("ids", ids), rs -> {
            parts.computeIfAbsent(uuid(rs, "package_version_id"), ignored -> new ArrayList<>())
                    .add(new PracticeTestCardResult.Part(uuid(rs, "id"), (Integer) rs.getObject("part_no"), rs.getString("title"),
                            rs.getInt("question_count"), strings(rs.getArray("formats"))));
        });
        return cards.stream().map(card -> new PracticeTestCardResult(card.packageId(), card.packageCode(), card.packageVersionId(),
                card.scope(), card.title(), card.skill(), card.coverReference(), card.sourceCode(), card.sourceName(),
                card.formats(), card.questionCount(), card.timeLimitSeconds(), card.requiredFeatureKey(),
                parts.getOrDefault(card.packageVersionId(), List.of()))).toList();
    }

    private PracticeTestCardResult card(ResultSet rs, int row) throws SQLException {
        String source = rs.getString("source_code");
        TestSource testSource = source == null ? null : TestSource.valueOf(source);
        String scope = rs.getObject("section_id") == null ? "FULL" : "PART";
        List<PracticeTestCardResult.Part> parts = scope.equals("PART") ? List.of(new PracticeTestCardResult.Part(
                uuid(rs, "section_id"), (Integer) rs.getObject("part_no"), rs.getString("title"),
                rs.getInt("question_count"), strings(rs.getArray("formats")))) : List.of();
        return new PracticeTestCardResult(uuid(rs, "package_id"), rs.getString("package_code"), uuid(rs, "version_id"),
                scope, rs.getString("title"), rs.getString("skill"), rs.getString("cover_reference"), source,
                testSource == null ? null : testSource.displayName(), strings(rs.getArray("formats")),
                rs.getInt("question_count"), (Integer) rs.getObject("time_limit_seconds"),
                rs.getString("required_feature_key"), parts);
    }

    private static List<String> strings(Array array) throws SQLException {
        if (array == null) return List.of();
        return Arrays.stream((Object[]) array.getArray()).map(Object::toString).toList();
    }
    private static UUID uuid(ResultSet rs, String column) throws SQLException { return rs.getObject(column, UUID.class); }
    private static String escape(String value) { return value.replace("!", "!!").replace("%", "!%").replace("_", "!_"); }
}
