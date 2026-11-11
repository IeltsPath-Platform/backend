package com.ieltspath.content.infrastructure.persistence;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers(disabledWithoutDocker = true)
class OutboxRemovalMigrationTest {
    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    @Test
    void upgradingTheOriginalContentSchemaRemovesItsUnusedOutboxAndPreservesCurriculum(@TempDir Path migrations) throws Exception {
        // V1 owns this table and no intervening migration changes it. Use the actual V1 and cleanup resources
        // without replaying unrelated catalog/course seeds; other migration suites cover the full current chain.
        copyMigration("V1__create_content_tables.sql", migrations);
        String location = "filesystem:" + migrations.toAbsolutePath().toString().replace('\\', '/');
        Flyway.configure().dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations(location).target("1").load().migrate();
        try (var connection = DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             var sql = connection.createStatement()) {
            assertNotNull(relation(sql, "outbox_events"));
            assertNotNull(relation(sql, "idx_outbox_events_published_created"));
            sql.execute("INSERT INTO outbox_events (aggregate_type, aggregate_id, event_type, payload) "
                    + "VALUES ('Content', 'legacy-content', 'LegacyContentEvent', '{}')");
            sql.execute("INSERT INTO topics (code, name) VALUES ('OUTBOX-MIGRATION-TOPIC', 'Preserved topic')");

            copyMigration("V124__drop_unused_outbox.sql", migrations);
            var flyway = Flyway.configure().dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                    .locations(location).load();
            assertEquals(1, flyway.migrate().migrationsExecuted);

            assertNull(relation(sql, "outbox_events"));
            assertNull(relation(sql, "idx_outbox_events_published_created"));
            assertNotNull(relation(sql, "idx_topics_parent_topic_id"));
            try (var rows = sql.executeQuery("SELECT name FROM topics WHERE code = 'OUTBOX-MIGRATION-TOPIC'")) {
                assertTrue(rows.next());
                assertEquals("Preserved topic", rows.getString(1));
            }
            assertEquals(0, flyway.migrate().migrationsExecuted);
        }
    }

    private static void copyMigration(String name, Path destination) throws Exception {
        try (var resource = OutboxRemovalMigrationTest.class.getResourceAsStream("/db/migration/" + name)) {
            assertNotNull(resource, "Missing migration resource: " + name);
            Files.copy(resource, destination.resolve(name));
        }
    }

    private static String relation(Statement sql, String name) throws Exception {
        try (var rows = sql.executeQuery("SELECT to_regclass('public." + name + "')")) {
            assertTrue(rows.next());
            return rows.getString(1);
        }
    }
}
