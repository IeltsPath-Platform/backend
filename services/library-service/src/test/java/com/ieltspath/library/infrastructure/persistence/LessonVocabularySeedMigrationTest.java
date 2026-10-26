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
class LessonVocabularySeedMigrationTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    @BeforeAll
    static void migrate() {
        Flyway.configure().dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration").load().migrate();
    }

    @Test
    void seedsTwentyFourNewItemsAndThirtyTwoActiveSensesWithoutDuplicateLemmas() throws Exception {
        // Six of the 30 lemmas (layer, plant, reach, rarely, slowly, pressure) already existed, so V15 reuses their items.
        assertEquals(24, count("vocabulary_items WHERE id::text LIKE '7b000000-0000-4000-8000-%'"));
        assertEquals(32, count("vocabulary_senses WHERE id::text LIKE '7a000000-0000-4000-8000-%' AND status = 'ACTIVE'"));
        assertEquals(1, count("vocabulary_items WHERE normalized_lemma = 'benefit'"));
        assertEquals(1, count("vocabulary_items WHERE normalized_lemma = 'drawback'"));
        assertEquals(0, count("(SELECT normalized_lemma FROM vocabulary_items WHERE normalized_lemma IN ('city planner', 'cope', 'hotter', 'green roof', 'layer', 'soil', 'plant', 'waterproof membrane', 'benefit', 'conventional', 'black roof', 'reach', 'planted roof', 'rarely', 'nearby', 'air conditioning', 'manage rainwater', 'soak up', 'heavy shower', 'release', 'slowly', 'pressure', 'city drain', 'copenhagen', 'convinced', 'critic', 'install', 'regular care', 'older building', 'carry', 'extra weight', 'drawback') GROUP BY normalized_lemma HAVING COUNT(*) > 1) duplicates"));
        assertEquals(0, count("vocabulary_senses WHERE id::text LIKE '7a000000-0000-4000-8000-%' AND (status <> 'ACTIVE' OR trim(vietnamese_meaning) = '' OR trim(example_sentence) = '')"));
    }

    @Test
    void rerunningMigrationDoesNotDuplicateSeedRows() throws Exception {
        executeSeed();

        assertEquals(24, count("vocabulary_items WHERE id::text LIKE '7b000000-0000-4000-8000-%'"));
        assertEquals(32, count("vocabulary_senses WHERE id::text LIKE '7a000000-0000-4000-8000-%'"));
        assertEquals(1, count("vocabulary_items WHERE normalized_lemma = 'benefit'"));
        assertEquals(1, count("vocabulary_items WHERE normalized_lemma = 'drawback'"));
    }

    @Test
    void everyLessonSenseHasAnActiveItemSoTheLessonApiReturnsIt() throws Exception {
        assertEquals(32, count("vocabulary_senses sense JOIN vocabulary_items item ON item.id = sense.vocabulary_item_id"
                + " WHERE sense.id::text LIKE '7a000000-0000-4000-8000-%'"
                + " AND sense.status = 'ACTIVE' AND item.status = 'ACTIVE'"));
    }

    @Test
    void readingLessonOneWordsAreActiveWithIpaAndAudioUrl() throws Exception {
        String lessonOneSenses = "('7a000000-0000-4000-8000-000000000001', '7a000000-0000-4000-8000-000000000002',"
                + " '7a000000-0000-4000-8000-000000000003', '7a000000-0000-4000-8000-000000000004',"
                + " '7a000000-0000-4000-8000-000000000005', '7a000000-0000-4000-8000-000000000006',"
                + " '7a000000-0000-4000-8000-000000000007', '7a000000-0000-4000-8000-000000000008')";

        assertEquals(8, count("vocabulary_senses sense JOIN vocabulary_items item ON item.id = sense.vocabulary_item_id"
                + " WHERE sense.id::text IN " + lessonOneSenses
                + " AND sense.status = 'ACTIVE' AND item.status = 'ACTIVE' AND item.ipa LIKE '/%/'"
                + " AND item.pronunciation_audio_reference LIKE 'https://%/media/vocabulary/pronunciation/us/%.mp3'"));
        assertEquals("https://kndrsdqzbzsaiuuruqno.supabase.co/storage/v1/object/public/media/vocabulary/pronunciation/us/city-planner.mp3",
                text("pronunciation_audio_reference FROM vocabulary_items WHERE normalized_lemma = 'city planner'"));
        assertEquals("/ˈleɪ.ɚ/", text("ipa FROM vocabulary_items WHERE normalized_lemma = 'layer'"));
    }

    private void executeSeed() throws Exception {
        try (var connection = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())) {
            ScriptUtils.executeSqlScript(connection,
                    new ClassPathResource("db/migration/V15__seed_lesson_vocabulary.sql"));
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
