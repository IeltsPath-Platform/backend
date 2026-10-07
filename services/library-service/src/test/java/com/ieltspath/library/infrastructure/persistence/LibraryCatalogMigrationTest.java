package com.ieltspath.library.infrastructure.persistence;

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
class LibraryCatalogMigrationTest {
    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    @BeforeAll
    static void migrate() {
        Flyway.configure().dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration").load().migrate();
    }

    @Test
    void createsOnlyCatalogTablesWithLogicalTopicReference() throws Exception {
        try (var connection = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             var statement = connection.createStatement()) {
            for (String table : List.of("vocabulary_items", "vocabulary_senses",
                    "learning_videos", "video_segments", "video_segment_lexical_entries")) {
                try (var rows = statement.executeQuery("SELECT to_regclass('public." + table + "')")) {
                    assertTrue(rows.next());
                    assertNotNull(rows.getString(1), table);
                }
            }
            try (var rows = statement.executeQuery(
                    "SELECT count(*) FROM information_schema.columns WHERE table_name = 'learning_videos' "
                            + "AND column_name = 'required_feature_key'")) {
                rows.next();
                assertEquals(1, rows.getInt(1));
            }
            try (var rows = statement.executeQuery(
                    "SELECT count(*) FROM pg_constraint WHERE conrelid = 'learning_videos'::regclass "
                            + "AND contype = 'f'")) {
                rows.next();
                assertEquals(0, rows.getInt(1));
            }
        }
    }
}
