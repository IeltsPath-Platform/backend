package com.ieltspath.content.infrastructure.persistence;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The Speaking grader reports only SPK_ codes, so each mistake it has to list needs one. */
@Testcontainers(disabledWithoutDocker = true)
class SpeakingErrorCodeMigrationTest {
    private static final List<String> GRAMMAR_CODES = List.of("SPK_TENSE_ERROR", "SPK_VERB_FORM",
            "SPK_SUBJECT_VERB_AGREEMENT", "SPK_PLURAL_ENDING", "SPK_ARTICLE_ERROR", "SPK_PREPOSITION_ERROR", "SPK_WORD_ORDER",
            "SPK_MISSING_SUBJECT_VERB", "SPK_RUN_ON_SENTENCE", "SPK_COMPLEX_STRUCTURE_ERROR");
    private static final List<String> VOCABULARY_CODES = List.of("SPK_WORD_CHOICE", "SPK_WORD_FORM", "SPK_L1_TRANSLATION",
            "SPK_REPETITIVE_WORD", "SPK_MISUSED_IDIOM", "SPK_WRONG_REGISTER");

    private static final List<String> COHERENCE_CODES = List.of("SPK_OFF_TOPIC_DIGRESSION", "SPK_REPEATED_IDEA",
            "SPK_UNDEVELOPED_IDEA");

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    private static NamedParameterJdbcTemplate jdbc;

    @BeforeAll
    static void migrate() {
        Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .load()
                .migrate();
        jdbc = new NamedParameterJdbcTemplate(new DriverManagerDataSource(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()));
    }

    @Test
    void grammarVocabularyAndCoherenceMistakesEachHaveAnApprovedActiveSpeakingCode() {
        for (String code : GRAMMAR_CODES) assertCode(code, "GRAMMAR");
        for (String code : VOCABULARY_CODES) assertCode(code, "VOCABULARY");
        for (String code : COHERENCE_CODES) assertCode(code, "COHERENCE");
    }

    @Test
    void theNewCodesOnlyExplainAResultAndAddNoEvidenceAgainstAKnowledgePoint() {
        Integer withTrap = jdbc.queryForObject("""
                SELECT count(*) FROM error_types
                WHERE code IN (:codes) AND (trap_knowledge_point_id IS NOT NULL OR knowledge_point_id IS NULL)
                """, Map.of("codes", java.util.stream.Stream.of(GRAMMAR_CODES, VOCABULARY_CODES, COHERENCE_CODES)
                        .flatMap(List::stream).toList()), Integer.class);

        assertEquals(0, withTrap);
    }

    private static void assertCode(String code, String category) {
        Map<String, Object> row = jdbc.queryForMap(
                "SELECT category, status, review_status, fix_hint FROM error_types WHERE code = :code", Map.of("code", code));

        assertEquals(category, row.get("category"), code);
        assertEquals("ACTIVE", row.get("status"), code);
        assertEquals("APPROVED", row.get("review_status"), code);
        assertTrue(row.get("fix_hint").toString().length() > 20, code);
    }
}
