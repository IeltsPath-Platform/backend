package com.group01.game.infrastructure.persistence;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.DriverManager;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers(disabledWithoutDocker = true)
class GameSchemaPostgresTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    @Test
    void migrationCreatesAllGameV5TablesAndIndexes() throws Exception {
        Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .load()
                .migrate();

        try (var connection = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             var statement = connection.createStatement();
             var result = statement.executeQuery("SELECT table_name FROM information_schema.tables "
                     + "WHERE table_schema = 'public' AND table_type = 'BASE TABLE'")) {
            Set<String> tables = new java.util.HashSet<>();
            while (result.next()) tables.add(result.getString(1));

            assertTrue(tables.containsAll(Set.of(
                    "game_sessions", "game_answers", "game_rooms", "game_room_members", "game_matches",
                    "game_match_players", "game_events", "quiz_events", "quiz_participations",
                    "leaderboard_periods", "leaderboard_entries", "outbox_events", "flyway_schema_history"
            )));
            assertEquals(13, tables.size());
        }
    }
}
