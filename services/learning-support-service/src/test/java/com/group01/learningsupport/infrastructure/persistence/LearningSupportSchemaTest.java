package com.group01.learningsupport.infrastructure.persistence;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Testcontainers(disabledWithoutDocker = true)
class LearningSupportSchemaTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    @BeforeAll
    static void migrate() {
        Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .load()
                .migrate();
    }

    @Test
    void createsNineTables() throws Exception {
        String sql = """
                SELECT table_name FROM information_schema.tables
                WHERE table_schema = 'public'
                  AND table_name IN (
                    'learning_activities', 'streaks', 'video_learning_progress',
                    'saved_video_segments', 'notes', 'flashcard_decks', 'flashcards',
                    'flashcard_deck_items', 'outbox_events'
                  )
                """;
        try (Connection connection = connection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet tables = statement.executeQuery()) {
            int count = 0;
            while (tables.next()) {
                count++;
            }
            assertEquals(9, count);
        }
    }

    @Test
    void createsListAndLookupIndexes() throws Exception {
        Set<String> expected = Set.of(
                "idx_learning_activities_user_occurred",
                "idx_video_learning_progress_user_updated",
                "idx_saved_video_segments_user_created",
                "idx_notes_user_status_updated",
                "idx_flashcard_decks_user_status_updated",
                "uq_flashcard_decks_user_name_not_deleted",
                "idx_flashcards_user_status_updated",
                "idx_flashcard_deck_items_deck_sort",
                "idx_flashcard_deck_items_flashcard",
                "idx_outbox_events_published_created"
        );
        String sql = """
                SELECT indexname FROM pg_indexes
                WHERE schemaname = 'public' AND indexname = ANY (?)
                """;
        try (Connection connection = connection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setArray(1, connection.createArrayOf("text", expected.toArray()));
            try (ResultSet indexes = statement.executeQuery()) {
                Set<String> found = new HashSet<>();
                while (indexes.next()) {
                    found.add(indexes.getString(1));
                }
                assertEquals(expected, found);
            }
        }
    }

    @Test
    void rejectsDuplicateVideoProgress() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID videoId = UUID.randomUUID();
        insertProgress(userId, videoId);
        assertThrows(SQLException.class, () -> insertProgress(userId, videoId));
    }

    @Test
    void deckNameUniqueIgnoresDeletedRows() throws Exception {
        UUID userId = UUID.randomUUID();
        insertDeck(userId, "Academic Vocabulary", "DELETED");
        insertDeck(userId, "Academic Vocabulary", "ACTIVE");
        assertThrows(SQLException.class, () -> insertDeck(userId, "Academic Vocabulary", "ACTIVE"));
    }

    private static void insertProgress(UUID userId, UUID videoId) throws SQLException {
        String sql = """
                INSERT INTO video_learning_progress (
                    id, user_id, video_id, last_position_ms, watched_duration_seconds,
                    progress_percent, status, updated_at
                ) VALUES (?, ?, ?, 0, 0, 0, 'NOT_STARTED', now())
                """;
        try (Connection connection = connection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, UUID.randomUUID());
            statement.setObject(2, userId);
            statement.setObject(3, videoId);
            statement.executeUpdate();
        }
    }

    private static void insertDeck(UUID userId, String name, String status) throws SQLException {
        String sql = """
                INSERT INTO flashcard_decks (id, user_id, name, status, created_at, updated_at)
                VALUES (?, ?, ?, ?, now(), now())
                """;
        try (Connection connection = connection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, UUID.randomUUID());
            statement.setObject(2, userId);
            statement.setString(3, name);
            statement.setString(4, status);
            statement.executeUpdate();
        }
    }

    private static Connection connection() throws SQLException {
        return DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    }
}
