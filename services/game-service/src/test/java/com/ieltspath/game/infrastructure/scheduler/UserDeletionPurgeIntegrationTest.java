package com.ieltspath.game.infrastructure.scheduler;

import com.ieltspath.game.infrastructure.persistence.JdbcDeletedUserStore;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Runs the account-deletion purge on real PostgreSQL with every Game migration, including a Wordly hint. */
@Testcontainers(disabledWithoutDocker = true)
class UserDeletionPurgeIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    static final Set<String> USER_COLUMNS = Set.of("user_id", "host_user_id");
    static final Set<String> KEPT_TABLES = Set.of("deleted_users", "user_deletion_jobs");

    private static JdbcTemplate jdbc;
    private static DriverManagerDataSource dataSource;
    private static RowSeeder seeder;

    @BeforeAll
    static void migrate() {
        dataSource = new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").load().migrate();
        jdbc = new JdbcTemplate(dataSource);
        seeder = new RowSeeder(jdbc);
    }

    @Test
    void everyTableWithAPlayerColumnIsHandled() {
        Set<String> handled = new HashSet<>(JdbcDeletedUserStore.HANDLED_TABLES);
        handled.addAll(KEPT_TABLES);
        Set<String> missing = new TreeSet<>(seeder.userColumns(USER_COLUMNS).keySet());
        missing.removeAll(handled);
        assertThat(missing).as("tables naming a player that account deletion does not handle").isEmpty();
    }

    @Test
    void purgeRemovesThePlayerAndKeepsTheRoomOthersPlayIn() {
        UUID deleted = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        UUID room = seeder.insert("game_rooms", Map.of("host_user_id", deleted));
        UUID match = seeder.insert("game_matches", Map.of("room_id", room));
        seedPlayer(deleted, room, match, 1);
        seedPlayer(other, room, match, 2);

        Map<String, List<String>> playerTables = seeder.userColumns(USER_COLUMNS);
        KEPT_TABLES.forEach(playerTables::remove);
        Map<String, Integer> othersBefore = seeder.rowsMentioning(playerTables, other);

        var store = new JdbcDeletedUserStore(jdbc);
        UUID eventId = UUID.randomUUID();
        store.recordDeletion(eventId, deleted, Instant.now());
        new UserDeletionJobWorker(store, new DataSourceTransactionManager(dataSource), 300, 10).processPending();

        assertThat(jdbc.queryForObject("SELECT status FROM user_deletion_jobs WHERE event_id = ?", String.class, eventId))
                .isEqualTo("DONE");
        assertThat(seeder.rowsMentioning(playerTables, deleted)).as("rows that still name the deleted player").isEmpty();
        assertThat(seeder.rowsMentioning(playerTables, other)).as("the other player's rows").isEqualTo(othersBefore);
        assertThat(jdbc.queryForObject("SELECT host_user_id FROM game_rooms WHERE id = ?", UUID.class, room))
                .isEqualTo(JdbcDeletedUserStore.DELETED_USER);
    }

    /** A player in the match with a Wordly word, a guess that gave a hint, a receipt, a Rocket answer and a session. */
    private static void seedPlayer(UUID player, UUID room, UUID match, int sequence) {
        UUID member = seeder.insert("game_room_members", Map.of("room_id", room, "user_id", player));
        UUID matchPlayer = seeder.insert("game_match_players", Map.of("match_id", match, "room_member_id", member,
                "user_id", player));
        UUID matchWord = seeder.insert("game_match_words", Map.of("match_id", match, "answer", "APPLE", "is_delete", false,
                "sequence_number", (int) (Math.random() * 100000) + 1));
        UUID playerWord = seeder.insert("game_player_words", Map.of("match_id", match, "match_player_id", matchPlayer,
                "match_word_id", matchWord));
        UUID guess = seeder.insert("game_guesses", Map.of("player_word_id", playerWord, "guess", "AB", "is_delete", false,
                "letter_results", "[\"CORRECT\", \"ABSENT\"]"));
        jdbc.update("""
                UPDATE game_player_words SET hint_request_id = ?, hint_source_guess_id = ?, hint_column = 0,
                    hint_letter = 'A', hint_used_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """, UUID.randomUUID(), guess, playerWord);
        seeder.insert("game_guess_receipts", Map.of("match_id", match, "player_word_id", playerWord, "user_id", player));
        seeder.insert("game_events", Map.of("match_id", match, "match_player_id", matchPlayer, "sequence_no", sequence));
        UUID round = seeder.insert("game_rocket_rounds", Map.of("match_id", match, "status", "PENDING", "is_delete", false,
                "options", "[\"a\", \"b\", \"c\"]", "sequence", sequence));
        seeder.insert("game_rocket_answers", Map.of("match_id", match, "round_id", round, "player_id", matchPlayer,
                "correct", false, "points", 0, "is_delete", false, "response_ms", 0));
        UUID session = seeder.insert("game_sessions", Map.of("user_id", player, "match_player_id", matchPlayer));
        seeder.insert("game_answers", Map.of("session_id", session));
    }
}
