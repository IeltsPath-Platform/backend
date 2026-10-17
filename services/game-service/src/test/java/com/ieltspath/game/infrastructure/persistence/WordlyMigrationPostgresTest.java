package com.ieltspath.game.infrastructure.persistence;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.DriverManager;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers(disabledWithoutDocker = true)
class WordlyMigrationPostgresTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    @Test
    void upgradesExistingRoomsAndEnforcesWordlyAndSoftDeletionConstraints() throws Exception {
        Flyway.configure().dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .target("1").load().migrate();
        try (var connection = DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             var sql = connection.createStatement()) {
            sql.execute("""
                INSERT INTO game_rooms(id, room_code, host_user_id, game_type, learning_domain,
                    mode, max_players, config_snapshot, status, created_at, updated_at)
                VALUES ('00000000-0000-0000-0000-000000000001', 'OLDROOM8', gen_random_uuid(),
                    'SPELLING', 'VOCABULARY', 'MULTIPLAYER', 8, '{}', 'WAITING', now(), now())
                """);
            Flyway.configure().dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                    .load().migrate();
            try (var rows = sql.executeQuery("SELECT room_code, max_players, is_delete, deleted_at FROM game_rooms")) {
                assertTrue(rows.next());
                assertEquals("OLDROOM8", rows.getString(1));
                assertEquals(8, rows.getInt(2));
                assertFalse(rows.getBoolean(3));
                assertNull(rows.getObject(4));
            }
            assertThrows(SQLException.class, () -> sql.execute("UPDATE game_rooms SET game_type = 'WORDLY'"));
            sql.execute("UPDATE game_rooms SET game_type = 'WORDLY', room_code = 'AB7K2Q', max_players = 4");
            assertThrows(SQLException.class, () -> sql.execute("UPDATE game_rooms SET initial_lives = 2"));
            assertThrows(SQLException.class, () -> sql.execute("UPDATE game_rooms SET word_time_limit_seconds = 60"));
            assertThrows(SQLException.class, () -> sql.execute("UPDATE game_rooms SET word_limit = 4"));
            assertThrows(SQLException.class, () -> sql.execute("UPDATE game_rooms SET play_mode = 'ENDLESS'"));
            sql.execute("UPDATE game_rooms SET play_mode = 'ENDLESS', word_limit = NULL");
            assertThrows(SQLException.class, () -> sql.execute("UPDATE game_rooms SET is_delete = TRUE"));
            sql.execute("UPDATE game_rooms SET is_delete = TRUE, deleted_at = now()");
            try (var rows = sql.executeQuery("""
                SELECT count(*) FROM information_schema.columns WHERE table_schema = 'public'
                AND table_name IN ('game_rooms', 'game_room_members', 'game_matches', 'game_match_players',
                    'game_match_words', 'game_player_words', 'game_guesses')
                AND column_name IN ('is_delete', 'deleted_at')
                """)) {
                assertTrue(rows.next());
                assertEquals(14, rows.getInt(1));
            }
        }
    }
}
