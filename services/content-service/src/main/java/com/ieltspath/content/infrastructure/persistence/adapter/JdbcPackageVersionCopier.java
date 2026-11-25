package com.ieltspath.content.infrastructure.persistence.adapter;

import com.ieltspath.content.application.port.PackageVersionCopier;
import jakarta.persistence.EntityManager;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Copies a version with {@code INSERT ... SELECT}. A copied row gets {@code md5(old id || target version id)} as its id:
 * unique per source row and target version, and the same expression maps a section or group id in the dependent rows,
 * so no id table is needed.
 */
@Component
public class JdbcPackageVersionCopier implements PackageVersionCopier {

    private static final String NEW_ID = "md5(%s::text || :target::text)::uuid";

    private final NamedParameterJdbcTemplate jdbc;
    private final EntityManager entityManager;

    public JdbcPackageVersionCopier(NamedParameterJdbcTemplate jdbc, EntityManager entityManager) {
        this.jdbc = jdbc;
        this.entityManager = entityManager;
    }

    @Override
    public void copyContent(UUID sourceVersionId, UUID targetVersionId) {
        // The target version row is written through JPA in this transaction; the copied rows reference it.
        entityManager.flush();
        var params = new MapSqlParameterSource("source", sourceVersionId).addValue("target", targetVersionId);

        jdbc.update("""
                INSERT INTO content_sections (id, package_version_id, title, skill, sort_order, time_limit_seconds,
                                              instructions, part_no, cover_asset_id)
                SELECT %s, :target, s.title, s.skill, s.sort_order, s.time_limit_seconds, s.instructions, s.part_no,
                       s.cover_asset_id
                FROM content_sections s WHERE s.package_version_id = :source
                """.formatted(NEW_ID.formatted("s.id")), params);

        jdbc.update("""
                INSERT INTO section_question_groups (id, section_id, sort_order, format_code, instructions)
                SELECT %s, %s, g.sort_order, g.format_code, g.instructions
                FROM section_question_groups g JOIN content_sections s ON s.id = g.section_id
                WHERE s.package_version_id = :source
                """.formatted(NEW_ID.formatted("g.id"), NEW_ID.formatted("g.section_id")), params);

        jdbc.update("""
                INSERT INTO section_questions (id, section_id, question_version_id, sort_order, max_score, group_id)
                SELECT %s, %s, q.question_version_id, q.sort_order, q.max_score,
                       CASE WHEN q.group_id IS NULL THEN NULL ELSE %s END
                FROM section_questions q JOIN content_sections s ON s.id = q.section_id
                WHERE s.package_version_id = :source
                """.formatted(NEW_ID.formatted("q.id"), NEW_ID.formatted("q.section_id"),
                NEW_ID.formatted("q.group_id")), params);

        jdbc.update("""
                INSERT INTO content_asset_links (id, asset_id, section_id, sort_order)
                SELECT %s, l.asset_id, %s, l.sort_order
                FROM content_asset_links l JOIN content_sections s ON s.id = l.section_id
                WHERE s.package_version_id = :source
                """.formatted(NEW_ID.formatted("l.id"), NEW_ID.formatted("l.section_id")), params);
    }
}
