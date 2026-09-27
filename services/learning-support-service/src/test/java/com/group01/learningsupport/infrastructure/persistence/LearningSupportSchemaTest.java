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
import static org.junit.jupiter.api.Assertions.assertNull;

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

    @Test
    void notesHaveNullableSourcePairConstraintAndIndex() throws Exception {
        try (Connection connection = connection()) {
            try (PreparedStatement columns = connection.prepareStatement("""
                    SELECT column_name, data_type FROM information_schema.columns
                    WHERE table_name = 'notes' AND column_name IN ('source_type', 'source_reference_id')
                    ORDER BY column_name
                    """);
                 ResultSet result = columns.executeQuery()) {
                result.next();
                assertEquals("source_reference_id", result.getString(1));
                assertEquals("uuid", result.getString(2));
                result.next();
                assertEquals("source_type", result.getString(1));
                assertEquals("character varying", result.getString(2));
                assertEquals(false, result.next());
            }
            try (PreparedStatement constraint = connection.prepareStatement("""
                    SELECT COUNT(*) FROM pg_constraint
                    WHERE conname = 'chk_notes_source_pair' AND conrelid = 'notes'::regclass
                    """);
                 ResultSet result = constraint.executeQuery()) {
                result.next();
                assertEquals(1, result.getInt(1));
            }
            try (PreparedStatement index = connection.prepareStatement("""
                    SELECT COUNT(*) FROM pg_indexes
                    WHERE tablename = 'notes' AND indexname = 'idx_notes_user_source'
                    """);
                 ResultSet result = index.executeQuery()) {
                result.next();
                assertEquals(1, result.getInt(1));
            }
        }
        UUID userId = UUID.randomUUID();
        assertThrows(SQLException.class, () ->
                insertNote(userId, "TUTOR_SESSION", null));
        assertThrows(SQLException.class, () ->
                insertNote(userId, null, UUID.randomUUID()));
    }

    @Test
    void oldNotesRemainReadableAndSourceFilterIsUserScoped() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        UUID sourceId = UUID.randomUUID();
        UUID oldId = insertNote(userId, null, null);
        UUID matchingId = insertNote(userId, "KNOWLEDGE_POINT", sourceId);
        insertNote(otherUserId, "KNOWLEDGE_POINT", sourceId);
        insertNote(userId, "TUTOR_SESSION", sourceId);

        try (Connection connection = connection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT source_type, source_reference_id FROM notes WHERE id = ?
                     """)) {
            statement.setObject(1, oldId);
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                assertNull(result.getString(1));
                assertNull(result.getObject(2));
            }
        }
        try (Connection connection = connection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT id FROM notes
                     WHERE user_id = ? AND status = 'ACTIVE'
                       AND source_type = ? AND source_reference_id = ?
                     ORDER BY updated_at DESC
                     """)) {
            statement.setObject(1, userId);
            statement.setString(2, "KNOWLEDGE_POINT");
            statement.setObject(3, sourceId);
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                assertEquals(matchingId, result.getObject(1));
                assertEquals(false, result.next());
            }
        }
    }

    private static UUID insertNote(UUID userId, String sourceType, UUID sourceReferenceId) throws SQLException {
        UUID id = UUID.randomUUID();
        try (Connection connection = connection();
             PreparedStatement statement = connection.prepareStatement("""
                     INSERT INTO notes (id, user_id, title, body, status, created_at, updated_at,
                                        source_type, source_reference_id)
                     VALUES (?, ?, 'Note', 'Body', 'ACTIVE', now(), now(), ?, ?)
                     """)) {
            statement.setObject(1, id);
            statement.setObject(2, userId);
            statement.setString(3, sourceType);
            statement.setObject(4, sourceReferenceId);
            statement.executeUpdate();
        }
        return id;
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
