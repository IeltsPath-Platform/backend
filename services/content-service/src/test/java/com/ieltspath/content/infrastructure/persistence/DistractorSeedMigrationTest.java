package com.ieltspath.content.infrastructure.persistence;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
class DistractorSeedMigrationTest {
    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");
    private static NamedParameterJdbcTemplate jdbc;

    @BeforeAll
    static void migrate() {
        var source = new PGSimpleDataSource();
        source.setURL(POSTGRES.getJdbcUrl());
        source.setUser(POSTGRES.getUsername());
        source.setPassword(POSTGRES.getPassword());
        // Later trap distractors are covered by the ErrorTrap migration and coverage tests.
        Flyway.configure().dataSource(source).locations("classpath:db/migration").target("78").load().migrate();
        jdbc = new NamedParameterJdbcTemplate(source);
    }

    @Test
    void mapsTwentyQuestionsWithThirtyOneWrongOptions() {
        assertThat(count("SELECT count(*) FROM question_versions WHERE answer_spec->'distractors' IS NOT NULL")).isEqualTo(20);
        assertThat(count("SELECT count(*) FROM question_versions qv CROSS JOIN LATERAL "
                + "jsonb_each_text(qv.answer_spec->'distractors') d")).isEqualTo(31);
    }

    @Test
    void everyDistractorKeyIsAnOptionOfItsOwnQuestionAndNeverTheCorrectOne() {
        assertThat(count("SELECT count(*) FROM question_versions qv CROSS JOIN LATERAL "
                + "jsonb_each_text(qv.answer_spec->'distractors') d WHERE NOT EXISTS "
                + "(SELECT 1 FROM jsonb_array_elements(qv.options) o WHERE o->>'optionKey' = d.key)")).isZero();
        assertThat(count("SELECT count(*) FROM question_versions WHERE answer_spec->'distractors'->>(answer_spec->>'correct') "
                + "IS NOT NULL")).isZero();
    }

    @Test
    void everyDistractorCodeIsAnActiveErrorType() {
        assertThat(count("SELECT count(*) FROM question_versions qv CROSS JOIN LATERAL "
                + "jsonb_each_text(qv.answer_spec->'distractors') d WHERE d.value NOT IN "
                + "(SELECT code FROM error_types WHERE status = 'ACTIVE')")).isZero();
    }

    @Test
    void theThreeReviewCodesAreUsedForTheirApprovedOptions() {
        assertThat(count("SELECT count(*) FROM question_versions qv CROSS JOIN LATERAL "
                + "jsonb_each_text(qv.answer_spec->'distractors') d WHERE d.value = 'CONTRADICTED_BY_PASSAGE'")).isEqualTo(4);
        assertThat(count("SELECT count(*) FROM question_versions qv CROSS JOIN LATERAL "
                + "jsonb_each_text(qv.answer_spec->'distractors') d WHERE d.value = 'DETAIL_AS_MAIN_IDEA'")).isEqualTo(1);
        assertThat(count("SELECT count(*) FROM question_versions qv CROSS JOIN LATERAL "
                + "jsonb_each_text(qv.answer_spec->'distractors') d WHERE d.value = 'WRONG_TIME_FRAME'")).isEqualTo(1);
    }

    @Test
    void theMappedQuestionsAreLearningReadingOrListening() {
        assertThat(count("SELECT count(*) FROM question_versions qv JOIN questions q ON q.id = qv.question_id "
                + "WHERE qv.answer_spec->'distractors' IS NOT NULL AND (q.purpose <> 'LEARNING' OR q.skill NOT IN ('READING', 'LISTENING'))"))
                .isZero();
    }

    @Test
    void theLaptopLoanQuestionMapsBothLibraryParagraphsToKeywordMatch() {
        // Paragraphs A and C repeat "library" without saying anything about cost, so both are keyword traps.
        Map<String, Object> row = jdbc.queryForMap("SELECT answer_spec->'distractors'->>'A' AS a, "
                + "answer_spec->'distractors'->>'C' AS c FROM question_versions "
                + "WHERE id = '26000000-0000-4000-8000-070000000013'", Map.of());
        assertThat(row).containsEntry("a", "KEYWORD_MATCH_ONLY").containsEntry("c", "KEYWORD_MATCH_ONLY");
    }

    private long count(String sql) {
        return jdbc.queryForObject(sql, Map.of(), Long.class);
    }
}
