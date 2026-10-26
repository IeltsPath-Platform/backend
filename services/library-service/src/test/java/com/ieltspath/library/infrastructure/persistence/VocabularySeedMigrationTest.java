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
class VocabularySeedMigrationTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    @BeforeAll
    static void migrate() {
        Flyway.configure().dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration").load().migrate();
    }

    // V4 seeds 300 mock items; V15 adds 30 lesson items and their 32 senses. V11 pronunciations are inactive.
    private static final String IELTS_ITEMS = "vocabulary_items WHERE status = 'ACTIVE'";
    private static final int ACTIVE_ITEM_COUNT = 330;
    private static final int SENSE_COUNT = 334;

    @Test
    void vocabularySeedsIncludeMockAndLessonContent() throws Exception {
        assertEquals(ACTIVE_ITEM_COUNT, count(IELTS_ITEMS));
        assertEquals(SENSE_COUNT, count("vocabulary_senses"));
    }

    @Test
    void dictationWordPronunciationsAreInactiveItemsWithoutSenses() throws Exception {
        assertEquals(0, count("vocabulary_items v WHERE v.status = 'INACTIVE' AND v.ipa IS NULL"));
        assertEquals(0, count("vocabulary_senses s JOIN vocabulary_items v ON v.id = s.vocabulary_item_id WHERE v.status = 'INACTIVE'"));
        assertEquals(1, count("vocabulary_items WHERE normalized_lemma = 'weekend' AND ipa = '/ˈwiˌkɛnd/' AND status = 'INACTIVE'"));
        assertEquals(1, count("vocabulary_items WHERE normalized_lemma = 'going'"), "one row per word");
    }

    @Test
    void rerunningSeedDoesNotDuplicateItemsOrSenses() throws Exception {
        assertEquals(ACTIVE_ITEM_COUNT, count(IELTS_ITEMS));
        assertEquals(SENSE_COUNT, count("vocabulary_senses"));

        try (var connection = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())) {
            ScriptUtils.executeSqlScript(connection,
                    new ClassPathResource("db/migration/V4__seed_mock_vocabulary.sql"));
        }

        assertEquals(ACTIVE_ITEM_COUNT, count(IELTS_ITEMS));
        assertEquals(SENSE_COUNT, count("vocabulary_senses"));
    }

    @Test
    void catalogMigrationsAddTopicsSetsAndMetadataWithoutAddingVocabularyRows() throws Exception {
        assertEquals(ACTIVE_ITEM_COUNT, count(IELTS_ITEMS));
        assertEquals(6, count("vocabulary_topics"));
        assertEquals(12, count("vocabulary_sets"));
        assertEquals(0, count("vocabulary_items WHERE frequency_rank IS NOT NULL"));
        assertEquals(3, count("vocabulary_items WHERE academic_word_list = true"));
        assertEquals(0, count("vocabulary_senses WHERE cefr_level IS NOT NULL AND topic_id IS NULL"));
        assertEquals(0, count("vocabulary_senses WHERE topic_id IS NOT NULL AND cefr_level IS NULL"));
        assertEquals(1, count("vocabulary_senses s JOIN vocabulary_items i ON i.id=s.vocabulary_item_id WHERE i.normalized_lemma='research' AND s.cefr_level='B2' AND s.topic_id IS NOT NULL"));
        assertEquals(16, count("vocabulary_senses s JOIN vocabulary_items i ON i.id=s.vocabulary_item_id WHERE s.cefr_level IS NOT NULL AND i.normalized_lemma IN ('pupil','curriculum','research','pollution','renewable','sustainable','device','algorithm','software','diet','stress','salary','community','policy','method','dissertation')"));
        assertEquals(32, count("vocabulary_examples e JOIN vocabulary_senses s ON s.id=e.sense_id JOIN vocabulary_items i ON i.id=s.vocabulary_item_id WHERE i.normalized_lemma IN ('pupil','curriculum','research','pollution','renewable','sustainable','device','algorithm','software','diet','stress','salary','community','policy','method','dissertation')"));
        assertEquals(1, count("vocabulary_set_items si JOIN vocabulary_sets st ON st.id=si.set_id WHERE st.kind='BAND' AND st.cefr_level='C2'"));
    }

    private int count(String table) throws Exception {
        try (var connection = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             var statement = connection.createStatement();
             var result = statement.executeQuery("SELECT count(*) FROM " + table)) {
            result.next();
            return result.getInt(1);
        }
    }
}
