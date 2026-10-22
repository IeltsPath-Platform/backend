package com.ieltspath.content.infrastructure.persistence;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

/** An active point authored after the seeds has no CEFR level or can-do, so the migration deactivates it. */
@Testcontainers
class KnowledgeGraphCompletionLegacyRowsMigrationTest {
    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    @Test
    void activePointWithoutMetadataIsDeactivatedAndKeepsItsData() throws Exception {
        migrateTo("72");
        try (Connection connection = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    INSERT INTO knowledge_points (id, topic_id, code, name, kind, learning_type, skill, description, status)
                    SELECT gen_random_uuid(), id, 'R65_AUTHOR_MADE', 'Author made', 'STRATEGY', 'PROCEDURE',
                           'READING', 'kept', 'ACTIVE'
                    FROM topics WHERE code = 'DEMO_READING'
                    """);
        }

        migrateTo("73");

        try (Connection connection = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("""
                     SELECT status, description, cefr_level, can_do FROM knowledge_points
                     WHERE code = 'R65_AUTHOR_MADE'
                     """)) {
            assertThat(result.next()).isTrue();
            assertThat(result.getString("status")).isEqualTo("INACTIVE");
            assertThat(result.getString("description")).isEqualTo("kept");
            assertThat(result.getString("cefr_level")).isNull();
            assertThat(result.getString("can_do")).isNull();
        }
    }

    private static void migrateTo(String version) {
        Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .target(version)
                .load()
                .migrate();
    }
}
