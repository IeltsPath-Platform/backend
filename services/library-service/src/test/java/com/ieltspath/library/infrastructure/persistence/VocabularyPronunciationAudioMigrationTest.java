package com.ieltspath.library.infrastructure.persistence;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.DriverManager;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Testcontainers
class VocabularyPronunciationAudioMigrationTest {
    private static final String KEYS = "vocabulary/pronunciation/";

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    @BeforeAll
    static void migrate() {
        Flyway.configure().dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration").load().migrate();
    }

    @Test
    void everyCatalogWordHasUkAndUsAudioKeys() throws Exception {
        assertEquals(0, count("vocabulary_items WHERE audio_uk_reference IS NULL OR audio_us_reference IS NULL"));
        assertEquals(0, count("vocabulary_items WHERE audio_uk_reference NOT LIKE '" + KEYS + "uk/%.mp3'"
                + " OR audio_us_reference NOT LIKE '" + KEYS + "us/%.mp3'"));
    }

    @Test
    void keyIsTheLemmaWithEveryRunOfOtherCharactersTurnedIntoADash() throws Exception {
        assertEquals(KEYS + "uk/green-roof.mp3", text("audio_uk_reference FROM vocabulary_items WHERE normalized_lemma = 'green roof'"));
        assertEquals(KEYS + "us/green-roof.mp3", text("audio_us_reference FROM vocabulary_items WHERE normalized_lemma = 'green roof'"));
        assertEquals(KEYS + "us/we-ve.mp3", text("audio_us_reference FROM vocabulary_items WHERE normalized_lemma = 'we''ve'"));
    }

    @Test
    void pronunciationAudioReferenceFromEarlierMigrationIsLeftAlone() throws Exception {
        assertEquals("https://kndrsdqzbzsaiuuruqno.supabase.co/storage/v1/object/public/media/" + KEYS + "us/city-planner.mp3",
                text("pronunciation_audio_reference FROM vocabulary_items WHERE normalized_lemma = 'city planner'"));
    }

    @Test
    void keyConversionLeavesUrlsOutsideTheMediaBucketAlone() throws Exception {
        execute("UPDATE vocabulary_items SET audio_uk_reference = 'https://cdn.example.com/uk/hot.mp3',"
                + " audio_us_reference = 'https://kndrsdqzbzsaiuuruqno.supabase.co/storage/v1/object/public/media/" + KEYS + "us/hot.mp3'"
                + " WHERE normalized_lemma = 'hot'");
        try (var connection = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())) {
            ScriptUtils.executeSqlScript(connection,
                    new ClassPathResource("db/migration/V34__vocabulary_audio_references_as_keys.sql"));

            assertEquals("https://cdn.example.com/uk/hot.mp3", text("audio_uk_reference FROM vocabulary_items WHERE normalized_lemma = 'hot'"));
            assertEquals(KEYS + "us/hot.mp3", text("audio_us_reference FROM vocabulary_items WHERE normalized_lemma = 'hot'"));
        } finally {
            // The container is shared with the other tests of this class.
            execute("UPDATE vocabulary_items SET audio_uk_reference = '" + KEYS + "uk/hot.mp3' WHERE normalized_lemma = 'hot'");
        }
    }

    private void execute(String sql) throws Exception {
        try (var connection = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             var statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        }
    }

    private String text(String selectAndFrom) throws Exception {
        try (var connection = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             var statement = connection.createStatement();
             var result = statement.executeQuery("SELECT " + selectAndFrom)) {
            result.next();
            return result.getString(1);
        }
    }

    private int count(String tableOrQuery) throws Exception {
        try (var connection = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             var statement = connection.createStatement();
             var result = statement.executeQuery("SELECT count(*) FROM " + tableOrQuery)) {
            result.next();
            return result.getInt(1);
        }
    }
}
