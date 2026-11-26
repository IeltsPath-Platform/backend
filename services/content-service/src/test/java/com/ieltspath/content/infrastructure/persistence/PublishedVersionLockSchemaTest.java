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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Runs the real migrations, so a column the lock names wrongly or no longer covers fails here: every content column of
 * a published version must refuse a new value.
 */
@Testcontainers(disabledWithoutDocker = true)
class PublishedVersionLockSchemaTest {
    private static final String LOCK_MESSAGE = "published_version_content_locked";

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    private static JdbcTemplate jdbc;

    @BeforeAll
    static void migrate() {
        Flyway.configure().dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration").load().migrate();
        var source = new PGSimpleDataSource();
        source.setURL(POSTGRES.getJdbcUrl());
        source.setUser(POSTGRES.getUsername());
        source.setPassword(POSTGRES.getPassword());
        jdbc = new JdbcTemplate(source);
    }

    @Test
    void everyContentColumnOfAPublishedQuestionVersionIsLocked() {
        assertLocked("question_versions", Map.of(
                "stem", "stem || ' changed'",
                "options", "'[\"changed\"]'::jsonb",
                "answer_spec", "'{\"changed\":true}'::jsonb",
                "schema_version", "schema_version + 1",
                "explanation", "coalesce(explanation, '') || ' changed'",
                "difficulty", "CASE WHEN difficulty = 'HARD' THEN 'EASY' ELSE 'HARD' END",
                "hint", "coalesce(hint, '') || ' changed'",
                "guide", "coalesce(guide, '') || ' changed'",
                "guide_steps", "'[\"changed\"]'::jsonb"));
    }

    @Test
    void everyContentColumnOfAPublishedPackageVersionIsLocked() {
        assertLocked("content_package_versions", Map.of(
                "rules", "'{\"changed\":true}'::jsonb",
                "schema_version", "schema_version + 1",
                "version_number", "version_number + 1000"));
    }

    private static void assertLocked(String table, Map<String, String> changes) {
        UUID id = jdbc.queryForObject("SELECT id FROM " + table + " WHERE status = 'PUBLISHED' LIMIT 1", UUID.class);
        changes.forEach((column, value) -> assertThatThrownBy(() ->
                jdbc.update("UPDATE " + table + " SET " + column + " = " + value + " WHERE id = ?", id))
                .as(table + "." + column)
                .hasMessageContaining(LOCK_MESSAGE));
        assertThat(jdbc.update("UPDATE " + table + " SET status = 'ARCHIVED' WHERE id = ?", id)).isEqualTo(1);
    }
}
