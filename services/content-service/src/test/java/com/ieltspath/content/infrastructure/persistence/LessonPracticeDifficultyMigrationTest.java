package com.ieltspath.content.infrastructure.persistence;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
class LessonPracticeDifficultyMigrationTest {
    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    private static NamedParameterJdbcTemplate jdbc;

    @BeforeAll
    static void migrate() {
        var source = new PGSimpleDataSource();
        source.setURL(POSTGRES.getJdbcUrl());
        source.setUser(POSTGRES.getUsername());
        source.setPassword(POSTGRES.getPassword());
        Flyway.configure().dataSource(source).locations("classpath:db/migration").load().migrate();
        jdbc = new NamedParameterJdbcTemplate(source);
    }

    @Test
    void lessonOwnedDemoQuestionsInheritTheUnambiguousCourseDifficulty() {
        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT p.code, qv.difficulty
                FROM content_packages p
                JOIN content_package_versions cpv ON cpv.package_id = p.id
                JOIN content_sections cs ON cs.package_version_id = cpv.id
                JOIN section_questions sq ON sq.section_id = cs.id
                JOIN question_versions qv ON qv.id = sq.question_version_id
                WHERE p.code IN ('PS-KP1-E', 'PS-KP2-D', 'PS-KP3-D', 'PS-KP4-D')
                ORDER BY p.code, qv.id
                """, Map.of());

        assertThat(rows).hasSize(12).allSatisfy(row -> assertThat(row.get("difficulty")).isEqualTo("EASY"));
        assertThat(rows).extracting(row -> row.get("code")).containsOnly(
                "PS-KP1-E", "PS-KP2-D", "PS-KP3-D", "PS-KP4-D");
    }
}
