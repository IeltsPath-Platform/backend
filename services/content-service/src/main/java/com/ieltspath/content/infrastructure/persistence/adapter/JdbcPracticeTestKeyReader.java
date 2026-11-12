package com.ieltspath.content.infrastructure.persistence.adapter;

import com.ieltspath.content.application.port.PracticeTestKeyReader;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class JdbcPracticeTestKeyReader implements PracticeTestKeyReader {
    private final NamedParameterJdbcTemplate jdbc;

    public JdbcPracticeTestKeyReader(NamedParameterJdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    public Keys find(List<UUID> packageVersionIds, List<UUID> questionVersionIds) {
        Map<UUID, UUID> packageByVersion = new HashMap<>();
        if (!packageVersionIds.isEmpty()) {
            jdbc.query("""
                    SELECT v.id, v.package_id
                    FROM content_package_versions v
                    JOIN content_packages p ON p.id = v.package_id
                    WHERE v.id IN (:ids) AND p.package_type = 'PRACTICE_TEST'
                    """, new MapSqlParameterSource("ids", packageVersionIds),
                    row -> { packageByVersion.put(row.getObject("id", UUID.class), row.getObject("package_id", UUID.class)); });
        }
        Map<UUID, QuestionKey> byQuestion = new HashMap<>();
        if (!questionVersionIds.isEmpty()) {
            // A question version can sit in more than one version of a package: the current published one decides.
            jdbc.query("""
                    SELECT DISTINCT ON (sq.question_version_id) sq.question_version_id, v.package_id, s.part_no
                    FROM section_questions sq
                    JOIN content_sections s ON s.id = sq.section_id
                    JOIN content_package_versions v ON v.id = s.package_version_id
                    JOIN content_packages p ON p.id = v.package_id
                    WHERE sq.question_version_id IN (:ids) AND p.package_type = 'PRACTICE_TEST' AND s.skill = 'WRITING'
                    ORDER BY sq.question_version_id, (p.current_published_version_id = v.id) DESC, v.version_number DESC
                    """, new MapSqlParameterSource("ids", questionVersionIds),
                    row -> {
                        byQuestion.put(row.getObject("question_version_id", UUID.class),
                                new QuestionKey(row.getObject("package_id", UUID.class), (Integer) row.getObject("part_no")));
                    });
        }
        return new Keys(packageByVersion, byQuestion);
    }
}
