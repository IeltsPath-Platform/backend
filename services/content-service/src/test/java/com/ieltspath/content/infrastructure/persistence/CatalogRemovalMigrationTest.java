package com.ieltspath.content.infrastructure.persistence;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.DriverManager;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers(disabledWithoutDocker = true)
class CatalogRemovalMigrationTest {
    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    @BeforeAll
    static void migrate() {
        Flyway.configure().dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration").load().migrate();
    }

    @Test
    void contentOwnsNoCatalogTables() throws Exception {
        try (var connection = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             var statement = connection.createStatement()) {
            for (String table : List.of("vocabulary_items", "vocabulary_senses",
                    "learning_videos", "video_segments", "video_segment_lexical_entries")) {
                try (var rows = statement.executeQuery("SELECT to_regclass('public." + table + "')")) {
                    assertTrue(rows.next());
                    assertNull(rows.getString(1), table);
                }
            }
        }
    }
}
