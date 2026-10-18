package com.ieltspath.content.infrastructure.persistence;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class KnowledgePointMetadataMigrationTest {
    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    @BeforeAll
    static void migrate() {
        Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .load()
                .migrate();
    }

    @Test
    void existingKnowledgePointsHaveNullableCefrAndScopeMetadataColumns() throws Exception {
        try (Connection connection = DriverManager.getConnection(
                     POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("""
                     SELECT cefr_level, can_do, scope_in, scope_out
                     FROM knowledge_points
                     ORDER BY created_at, id
                     LIMIT 1
                     """)) {
            assertThat(result.next()).isTrue();
            assertThat(result.getString("cefr_level")).isNull();
            assertThat(result.getString("can_do")).isNull();
            assertThat(result.getString("scope_in")).isNull();
            assertThat(result.getString("scope_out")).isNull();
        }
    }
}
