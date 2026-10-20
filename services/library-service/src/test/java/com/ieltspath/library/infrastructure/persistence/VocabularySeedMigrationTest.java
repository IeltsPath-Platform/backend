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

    // The INACTIVE items are the pronunciations of the dictation words (V11), not part of the IELTS seed.
    private static final String IELTS_ITEMS = "vocabulary_items WHERE status = 'ACTIVE'";

    @Test
    void seedAddsThreeHundredItemsAndThreeHundredTwoSenses() throws Exception {
        assertEquals(300, count(IELTS_ITEMS));
        assertEquals(302, count("vocabulary_senses"));
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
        assertEquals(300, count(IELTS_ITEMS));
        assertEquals(302, count("vocabulary_senses"));

        try (var connection = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())) {
            ScriptUtils.executeSqlScript(connection,
                    new ClassPathResource("db/migration/V4__seed_mock_vocabulary.sql"));
        }

        assertEquals(300, count(IELTS_ITEMS));
        assertEquals(302, count("vocabulary_senses"));
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
