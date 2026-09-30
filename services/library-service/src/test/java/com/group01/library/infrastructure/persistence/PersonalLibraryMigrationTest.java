package com.group01.library.infrastructure.persistence;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers(disabledWithoutDocker = true)
class PersonalLibraryMigrationTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    @BeforeAll
    static void migrate() {
        Flyway.configure().dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration").load().migrate();
    }

    @Test
    void createsSixPersonalTablesAndPreservesIndexesWithoutOutbox() throws Exception {
        try (Connection connection = connection(); var statement = connection.createStatement()) {
            for (String table : List.of("flashcard_decks", "flashcards", "flashcard_deck_items", "notes",
                    "video_learning_progress", "saved_video_segments")) {
                try (var rows = statement.executeQuery("SELECT to_regclass('public." + table + "')")) {
                    assertTrue(rows.next());
                    assertNotNull(rows.getString(1), table);
                }
            }
            try (var rows = statement.executeQuery("SELECT to_regclass('public.outbox_events')")) {
                assertTrue(rows.next());
                assertNull(rows.getString(1));
            }
            for (String index : List.of("uq_flashcards_user_practice_question",
                    "uq_flashcard_decks_user_name_not_deleted", "idx_notes_user_source",
                    "idx_video_learning_progress_user_updated", "idx_saved_video_segments_user_created",
                    "idx_notes_user_status_updated", "idx_flashcard_decks_user_status_updated",
                    "idx_flashcards_user_status_updated", "idx_flashcard_deck_items_deck_sort",
                    "idx_flashcard_deck_items_flashcard")) {
                try (var rows = statement.executeQuery("SELECT to_regclass('public." + index + "')")) {
                    assertTrue(rows.next());
                    assertNotNull(rows.getString(1), index);
                }
            }
            for (String constraint : List.of("fk_flashcards_vocabulary_sense",
                    "fk_video_learning_progress_video", "fk_saved_video_segments_video",
                    "fk_saved_video_segments_segment")) {
                try (var rows = statement.executeQuery("SELECT confdeltype FROM pg_constraint WHERE conname = '"
                        + constraint + "'")) {
                    assertTrue(rows.next(), constraint);
                    assertEquals("r", rows.getString(1), constraint);
                    assertFalse(rows.next(), constraint);
                }
            }
        }
    }

    @Test
    void catalogReferencesRestrictDeletion() throws Exception {
        UUID item = UUID.randomUUID();
        UUID sense = UUID.randomUUID();
        UUID progressVideo = UUID.randomUUID();
        UUID savedVideo = UUID.randomUUID();
        UUID segment = UUID.randomUUID();
        try (Connection connection = connection(); var statement = connection.createStatement()) {
            statement.executeUpdate("INSERT INTO vocabulary_items (id, lemma, normalized_lemma) VALUES ('"
                    + item + "', 'word', 'word')");
            statement.executeUpdate("INSERT INTO vocabulary_senses "
                    + "(id, vocabulary_item_id, part_of_speech, vietnamese_meaning, example_sentence) VALUES ('"
                    + sense + "', '" + item + "', 'NOUN', 'meaning', 'example')");
            statement.executeUpdate("INSERT INTO flashcards (id, user_id, source_type, vocabulary_sense_id, front, back) "
                    + "VALUES ('" + UUID.randomUUID() + "', '" + UUID.randomUUID() + "', 'VOCABULARY_SENSE', '"
                    + sense + "', 'front', 'back')");
            for (UUID video : List.of(progressVideo, savedVideo)) {
                statement.executeUpdate("INSERT INTO learning_videos (id, youtube_video_id, youtube_url, title) VALUES ('"
                        + video + "', '" + video.toString().substring(0, 16) + "', 'https://example.test', 'video')");
            }
            statement.executeUpdate("INSERT INTO video_learning_progress (id, user_id, video_id) VALUES ('"
                    + UUID.randomUUID() + "', '" + UUID.randomUUID() + "', '" + progressVideo + "')");
            statement.executeUpdate("INSERT INTO video_segments (id, video_id, sequence_no, start_ms, end_ms, transcript) "
                    + "VALUES ('" + segment + "', '" + savedVideo + "', 1, 0, 100, 'text')");
            statement.executeUpdate("INSERT INTO saved_video_segments "
                    + "(id, user_id, video_id, segment_id, transcript_snapshot) VALUES ('"
                    + UUID.randomUUID() + "', '" + UUID.randomUUID() + "', '" + savedVideo + "', '"
                    + segment + "', 'text')");
        }
        assertDeleteRejected("vocabulary_senses", sense);
        assertDeleteRejected("learning_videos", progressVideo);
        assertDeleteRejected("learning_videos", savedVideo);
        assertDeleteRejected("video_segments", segment);
    }

    private static void assertDeleteRejected(String table, UUID id) throws Exception {
        try (Connection connection = connection(); var statement = connection.createStatement()) {
            assertThrows(SQLException.class, () -> statement.executeUpdate(
                    "DELETE FROM " + table + " WHERE id = '" + id + "'"));
        }
    }

    private static Connection connection() throws SQLException {
        return DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    }
}
