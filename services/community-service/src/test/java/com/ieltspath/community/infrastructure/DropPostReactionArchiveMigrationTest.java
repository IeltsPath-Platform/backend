package com.ieltspath.community.infrastructure;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.DriverManager;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers(disabledWithoutDocker = true)
class DropPostReactionArchiveMigrationTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    private static final Set<String> DROPPED = Set.of("post_reaction_archive");
    private static final Set<String> KEPT = Set.of("posts", "comments", "post_reactions", "comment_reactions", "outbox_events", "deleted_users", "user_deletion_jobs", "flyway_schema_history");

    @Test
    void removesTheUnusedTablesAndKeepsTheRest() throws Exception {
        Flyway.configure().dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration").load().migrate();

        Set<String> tables = new HashSet<>();
        try (var connection = DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             var statement = connection.createStatement();
             var rows = statement.executeQuery("SELECT table_name FROM information_schema.tables "
                     + "WHERE table_schema = 'public' AND table_type = 'BASE TABLE'")) {
            while (rows.next()) tables.add(rows.getString(1));
        }

        for (String dropped : DROPPED) {
            assertTrue(!tables.contains(dropped), dropped + " must be gone");
        }
        assertTrue(tables.containsAll(KEPT), "tables that are still in use must remain");
        assertEquals(11, tables.size()); // 10 tables plus flyway_schema_history
    }
}
