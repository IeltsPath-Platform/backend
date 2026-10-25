package com.ieltspath.content.infrastructure.persistence;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
class ErrorTrapCoverageMigrationTest {
    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");
    private static JdbcTemplate jdbc;

    @BeforeAll
    static void migrate() {
        var source = new PGSimpleDataSource();
        source.setURL(POSTGRES.getJdbcUrl());
        source.setUser(POSTGRES.getUsername());
        source.setPassword(POSTGRES.getPassword());
        Flyway.configure().dataSource(source).locations("classpath:db/migration").load().migrate();
        jdbc = new JdbcTemplate(source);
    }

    @Test
    void everyInScopeErrorHasTwoPracticeSetsWithAtLeastTwoMatchingTraps() {
        var scope = jdbc.queryForList("""
                SELECT et.code, parent.code AS parent_code
                FROM error_types et
                JOIN knowledge_points trap ON trap.id = et.trap_knowledge_point_id AND trap.status = 'ACTIVE'
                JOIN knowledge_points parent ON parent.id = et.knowledge_point_id AND parent.status = 'ACTIVE'
                WHERE et.status = 'ACTIVE'
                  AND et.code NOT IN ('WORD_LIMIT_EXCEEDED', 'NUMBER_FORMAT', 'PLURAL_FORM', 'SPELLING')
                  AND EXISTS (
                      SELECT 1 FROM question_knowledge_points qkp
                      JOIN question_versions qv ON qv.id = qkp.question_version_id AND qv.status = 'PUBLISHED'
                      WHERE qkp.knowledge_point_id = et.knowledge_point_id AND qv.answer_spec->>'type' = 'CHOICE'
                  )
                ORDER BY et.code
                """);
        assertThat(scope).isNotEmpty();
        var gaps = jdbc.queryForList("""
                WITH in_scope AS (
                    SELECT et.code, et.knowledge_point_id AS parent_id
                    FROM error_types et
                    JOIN knowledge_points trap ON trap.id = et.trap_knowledge_point_id AND trap.status = 'ACTIVE'
                    JOIN knowledge_points parent ON parent.id = et.knowledge_point_id AND parent.status = 'ACTIVE'
                    WHERE et.status = 'ACTIVE'
                      AND et.code NOT IN ('WORD_LIMIT_EXCEEDED', 'NUMBER_FORMAT', 'PLURAL_FORM', 'SPELLING')
                      AND EXISTS (
                          SELECT 1 FROM question_knowledge_points qkp
                          JOIN question_versions qv ON qv.id = qkp.question_version_id AND qv.status = 'PUBLISHED'
                          WHERE qkp.knowledge_point_id = et.knowledge_point_id
                            AND qv.answer_spec->>'type' = 'CHOICE'
                      )
                ), package_stats AS (
                    SELECT e.code, p.id AS package_id,
                           count(DISTINCT sq.question_version_id) AS total_questions,
                           count(DISTINCT sq.question_version_id) FILTER (WHERE EXISTS (
                               SELECT 1 FROM question_knowledge_points qkp
                               WHERE qkp.question_version_id = sq.question_version_id AND qkp.knowledge_point_id = e.parent_id
                           )) AS parent_questions,
                           count(DISTINCT sq.question_version_id) FILTER (WHERE EXISTS (
                               SELECT 1 FROM question_versions qv
                               CROSS JOIN LATERAL jsonb_each_text(COALESCE(qv.answer_spec->'distractors', '{}'::jsonb)) d
                               WHERE qv.id = sq.question_version_id AND d.value = e.code
                           )) AS trap_questions
                    FROM in_scope e
                    JOIN content_packages p ON p.package_type = 'PRACTICE_SET' AND p.status = 'PUBLISHED'
                    JOIN content_sections s ON s.package_version_id = p.current_published_version_id
                    JOIN section_questions sq ON sq.section_id = s.id
                    GROUP BY e.code, p.id
                ), eligible_sets AS (
                    SELECT code, count(*) FILTER (WHERE total_questions >= 3 AND parent_questions >= 1 AND trap_questions >= 2) AS set_count
                    FROM package_stats GROUP BY code
                )
                SELECT e.code, COALESCE(es.set_count, 0) AS eligible_sets
                FROM in_scope e LEFT JOIN eligible_sets es ON es.code = e.code
                WHERE COALESCE(es.set_count, 0) < 2
                ORDER BY e.code
                """);

        assertThat(gaps).as("in-scope error codes below the practice-set coverage threshold").isEmpty();
    }

    @Test
    void distractorMappingsUseRealOptionsAndActiveErrorsAndQuestionsAvoidTrapPoints() {
        var invalidMappings = jdbc.queryForList("""
                SELECT qv.id, d.key AS option_key, d.value AS error_code, qv.options::text AS options
                FROM question_versions qv
                CROSS JOIN LATERAL jsonb_each_text(COALESCE(qv.answer_spec->'distractors', '{}'::jsonb)) d
                LEFT JOIN error_types et ON et.code = d.value AND et.status = 'ACTIVE'
                WHERE qv.status = 'PUBLISHED'
                  AND (et.code IS NULL OR NOT EXISTS (
                      SELECT 1 FROM jsonb_array_elements(COALESCE(qv.options, '[]'::jsonb)) o
                      WHERE o->>'optionKey' = d.key
                  ))
                """);
        Integer trapAssignedQuestions = jdbc.queryForObject("""
                SELECT count(DISTINCT qkp.question_version_id)
                FROM question_knowledge_points qkp
                JOIN error_types et ON et.trap_knowledge_point_id = qkp.knowledge_point_id
                """, Integer.class);

        assertThat(invalidMappings).isEmpty();
        assertThat(trapAssignedQuestions).isZero();
    }

    @Test
    void twoSetsForOneErrorCodeDoNotRepeatQuestionStems() {
        var duplicateStems = jdbc.queryForList("""
                SELECT regexp_replace(p.code, '-[0-9]+$', '') AS error_package,
                       replace(qv.stem, '[[place]]', '') AS normalized_stem,
                       count(DISTINCT p.id) AS package_count
                FROM content_packages p
                JOIN content_sections s ON s.package_version_id = p.current_published_version_id
                JOIN section_questions sq ON sq.section_id = s.id
                JOIN question_versions qv ON qv.id = sq.question_version_id
                WHERE p.code LIKE 'PS-TRAP-%'
                GROUP BY regexp_replace(p.code, '-[0-9]+$', ''), replace(qv.stem, '[[place]]', '')
                HAVING count(DISTINCT p.id) > 1
                """);

        assertThat(duplicateStems).as("no question stem is reused between sets for the same error code").isEmpty();
    }

    @Test
    void multipleChoiceAnswerKeysVaryWithinEachPracticeSet() {
        var imbalancedSets = jdbc.queryForList("""
                WITH key_counts AS (
                    SELECT p.code, qv.answer_spec->>'correct' AS correct_key, count(*) AS answer_count
                    FROM content_packages p
                    JOIN content_sections s ON s.package_version_id = p.current_published_version_id
                    JOIN section_questions sq ON sq.section_id = s.id
                    JOIN question_versions qv ON qv.id = sq.question_version_id
                    JOIN questions q ON q.id = qv.question_id
                    WHERE p.code LIKE 'PS-TRAP-%' AND q.question_type = 'MULTIPLE_CHOICE'
                    GROUP BY p.code, qv.answer_spec->>'correct'
                )
                SELECT code, sum(answer_count) AS questions, max(answer_count) AS most_common_key_count
                FROM key_counts
                GROUP BY code
                HAVING sum(answer_count) >= 3 AND max(answer_count) * 2 > sum(answer_count)
                """);

        assertThat(imbalancedSets).as("no correct option key occurs in more than half of a multiple-choice set").isEmpty();
    }

    @Test
    void trueFalseNotGivenTrapCodesMeetTheirSeparateThresholds() {
        for (var code : List.of("TFNG_NG_AS_FALSE", "TFNG_FALSE_AS_NG")) {
            var correct = code.equals("TFNG_NG_AS_FALSE") ? "NOT_GIVEN" : "FALSE";
            Integer sets = jdbc.queryForObject("""
                    SELECT count(*) FROM (
                        SELECT p.id
                        FROM content_packages p
                        JOIN content_sections s ON s.package_version_id = p.current_published_version_id
                        JOIN section_questions sq ON sq.section_id = s.id
                        JOIN question_versions qv ON qv.id = sq.question_version_id AND qv.status = 'PUBLISHED'
                        JOIN questions q ON q.id = qv.question_id AND q.question_type = 'TRUE_FALSE_NOT_GIVEN'
                        WHERE p.package_type = 'PRACTICE_SET' AND p.status = 'PUBLISHED'
                          AND EXISTS (SELECT 1 FROM jsonb_each_text(COALESCE(qv.answer_spec->'distractors', '{}'::jsonb)) d WHERE d.value = ?)
                          AND qv.answer_spec->>'correct' = ?
                        GROUP BY p.id
                        HAVING count(DISTINCT qv.id) FILTER (WHERE EXISTS (
                            SELECT 1 FROM jsonb_each_text(COALESCE(qv.answer_spec->'distractors', '{}'::jsonb)) d WHERE d.value = ?
                        )) >= 2
                    ) covered
                    """, Integer.class, code, correct, code);
            var observed = jdbc.queryForList("""
                    SELECT q.question_type, qv.answer_spec->>'correct' AS correct_key, d.value AS error_code, count(*) AS questions
                    FROM content_packages p
                    JOIN content_sections s ON s.package_version_id = p.current_published_version_id
                    JOIN section_questions sq ON sq.section_id = s.id
                    JOIN question_versions qv ON qv.id = sq.question_version_id
                    JOIN questions q ON q.id = qv.question_id
                    CROSS JOIN LATERAL jsonb_each_text(COALESCE(qv.answer_spec->'distractors', '{}'::jsonb)) d
                    WHERE p.code LIKE 'PS-TRAP-%' AND d.value = ?
                    GROUP BY q.question_type, qv.answer_spec->>'correct', d.value
                    """, code);
            assertThat(sets).as("practice sets with at least two %s traps; observed %s", code, observed)
                    .isGreaterThanOrEqualTo(2);

            var unbalancedTfngSets = jdbc.queryForList("""
                    SELECT p.code,
                           count(*) FILTER (WHERE q.question_type = 'TRUE_FALSE_NOT_GIVEN') AS tfng_count,
                           count(*) FILTER (WHERE q.question_type = 'TRUE_FALSE_NOT_GIVEN'
                               AND qv.answer_spec->>'correct' = 'TRUE'
                            AND NOT (qv.answer_spec ?? 'distractors')) AS untagged_true_count
                    FROM content_packages p
                    JOIN content_sections s ON s.package_version_id = p.current_published_version_id
                    JOIN section_questions sq ON sq.section_id = s.id
                    JOIN question_versions qv ON qv.id = sq.question_version_id
                    JOIN questions q ON q.id = qv.question_id
                    WHERE p.code LIKE ?
                    GROUP BY p.code
                    HAVING count(*) FILTER (WHERE q.question_type = 'TRUE_FALSE_NOT_GIVEN') <> 4
                        OR count(*) FILTER (WHERE q.question_type = 'TRUE_FALSE_NOT_GIVEN'
                            AND qv.answer_spec->>'correct' = 'TRUE'
                            AND NOT (qv.answer_spec ?? 'distractors')) < 1
                    """, "PS-TRAP-" + code + "-%");
            assertThat(unbalancedTfngSets).as("every T/F/NG set contains four T/F/NG questions and an untagged TRUE")
                    .isEmpty();
        }
    }
}
