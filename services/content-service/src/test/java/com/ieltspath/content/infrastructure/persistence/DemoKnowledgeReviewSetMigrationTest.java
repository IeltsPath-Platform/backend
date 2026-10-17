package com.ieltspath.content.infrastructure.persistence;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
class DemoKnowledgeReviewSetMigrationTest {
    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    private static JdbcTemplate jdbc;

    @BeforeAll
    static void migrate() {
        var source = new PGSimpleDataSource();
        source.setURL(POSTGRES.getJdbcUrl());
        source.setUser(POSTGRES.getUsername());
        source.setPassword(POSTGRES.getPassword());
        Flyway.configure().dataSource(source).locations("classpath:db/migration").target("40").load().migrate();
        jdbc = new JdbcTemplate(source);
    }

    @Test
    void migrationPublishesUnrevealedPracticeSetsForEveryDemoReadingKnowledgePoint() {
        var rows = jdbc.queryForList("""
                SELECT p.code, p.status AS package_status, kp.code AS knowledge_point_code,
                       count(DISTINCT sq.id) AS question_count,
                       count(DISTINCT asset.id) AS passage_count,
                       count(DISTINCT CASE WHEN qv.status='PUBLISHED'
                           AND q.current_published_version_id=qv.id THEN qv.id END) AS current_published_question_count
                FROM content_packages p
                JOIN content_package_versions pv ON pv.id=p.current_published_version_id
                JOIN content_sections section ON section.package_version_id=pv.id
                JOIN content_asset_links asset_link ON asset_link.section_id=section.id
                JOIN content_assets asset ON asset.id=asset_link.asset_id AND asset.asset_type='PASSAGE'
                JOIN section_questions sq ON sq.section_id=section.id
                JOIN question_versions qv ON qv.id=sq.question_version_id
                JOIN questions q ON q.id=qv.question_id
                JOIN question_knowledge_points qkp ON qkp.question_version_id=qv.id
                JOIN knowledge_points kp ON kp.id=qkp.knowledge_point_id
                WHERE p.code IN ('PS-KP1-E','PS-KP2-D','PS-KP3-D','PS-KP4-D')
                GROUP BY p.code,p.status,kp.code
                """);

        assertThat(rows).hasSize(4);
        assertThat(rows).allSatisfy(row -> {
            String code = (String) row.get("code");
            assertThat(row.get("package_status")).as(code).isEqualTo("PUBLISHED");
            assertThat(((Number) row.get("question_count")).intValue()).as(code).isEqualTo(3);
            assertThat(((Number) row.get("passage_count")).intValue()).as(code).isEqualTo(1);
            assertThat(((Number) row.get("current_published_question_count")).intValue()).as(code).isEqualTo(3);
        });

        Map<String, String> expectedKnowledgePoints = Map.of(
                "PS-KP1-E", "DEMO_READING_MAIN_IDEA",
                "PS-KP2-D", "DR_IDEA_OR_DETAIL",
                "PS-KP3-D", "DR_TOPIC_SENTENCE",
                "PS-KP4-D", "DR_MATCHING_HEADINGS");
        assertThat(rows).allSatisfy(row -> {
            String code = (String) row.get("code");
            assertThat(row.get("knowledge_point_code")).as(code).isEqualTo(expectedKnowledgePoints.get(code));
        });
    }
}
