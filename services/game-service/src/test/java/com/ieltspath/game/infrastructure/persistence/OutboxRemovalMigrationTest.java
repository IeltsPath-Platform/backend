package com.ieltspath.game.infrastructure.persistence;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.DriverManager;
import java.sql.Statement;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers(disabledWithoutDocker = true)
class OutboxRemovalMigrationTest {
    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    @Test
    void upgradingAnExistingDatabaseDropsOnlyTheUnusedOutboxAndItsIndex() throws Exception {
        Flyway.configure().dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration").target("12").load().migrate();
        UUID sessionId = UUID.randomUUID(), matchId = UUID.randomUUID(), eventId = UUID.randomUUID();
        try (var connection = DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             var sql = connection.createStatement()) {
            assertNotNull(relation(sql, "outbox_events"));
            assertNotNull(relation(sql, "idx_outbox_events_pending"));
            sql.execute("INSERT INTO outbox_events (id, aggregate_type, aggregate_id, event_type, payload, created_at) "
                    + "VALUES (gen_random_uuid(), 'GameSession', 'legacy-session', 'GameSessionStarted', '{}', now())");
            sql.execute("INSERT INTO game_sessions (id, user_id, game_type, learning_domain, mode, source_snapshot, started_at, status) "
                    + "VALUES ('" + sessionId + "', gen_random_uuid(), 'WORD_MEANING_MATCH', 'VOCABULARY', 'PRACTICE', '{}', now(), 'IN_PROGRESS')");
            sql.execute("INSERT INTO game_matches (id, game_type, learning_domain, config_snapshot, content_snapshot, status, created_at) "
                    + "VALUES ('" + matchId + "', 'WORD_MEANING_MATCH', 'VOCABULARY', '{}', '{}', 'IN_PROGRESS', now())");
            sql.execute("INSERT INTO game_events (id, match_id, sequence_no, event_type, payload, occurred_at, created_at) "
                    + "VALUES ('" + eventId + "', '" + matchId + "', 1, 'ANSWER_SUBMITTED', '{}', now(), now())");

            var flyway = Flyway.configure().dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                    .locations("classpath:db/migration").load();
            flyway.migrate();

            assertNull(relation(sql, "outbox_events"));
            assertNull(relation(sql, "idx_outbox_events_pending"));
            assertNotNull(relation(sql, "idx_game_events_match_sequence"));
            try (var rows = sql.executeQuery("SELECT status FROM game_sessions WHERE id = '" + sessionId + "'")) {
                assertTrue(rows.next());
                assertEquals("IN_PROGRESS", rows.getString(1));
            }
            try (var rows = sql.executeQuery("SELECT event_type FROM game_events WHERE id = '" + eventId + "'")) {
                assertTrue(rows.next());
                assertEquals("ANSWER_SUBMITTED", rows.getString(1));
            }
            assertEquals(0, flyway.migrate().migrationsExecuted);
        }
    }

    private static String relation(Statement sql, String name) throws Exception {
        try (var rows = sql.executeQuery("SELECT to_regclass('public." + name + "')")) {
            assertTrue(rows.next());
            return rows.getString(1);
        }
    }
}
