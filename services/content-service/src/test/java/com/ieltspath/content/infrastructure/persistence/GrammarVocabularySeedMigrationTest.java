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
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class GrammarVocabularySeedMigrationTest {
    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");
    private static NamedParameterJdbcTemplate jdbc;

    @BeforeAll
    static void migrate() {
        var source = new PGSimpleDataSource();
        source.setURL(POSTGRES.getJdbcUrl());
        source.setUser(POSTGRES.getUsername());
        source.setPassword(POSTGRES.getPassword());
        Flyway.configure().dataSource(source).locations("classpath:db/migration").target("55").load().migrate();
        jdbc = new NamedParameterJdbcTemplate(source);
    }

    @Test
    void seedsApprovedGrammarAndVocabularyPointsAsInactiveWithExpectedCefr() {
        var points = jdbc.queryForList("""
                SELECT code, kind, skill, status, learning_type, cefr_level
                FROM knowledge_points
                WHERE code LIKE 'G55_%' OR code = 'G60_PERFECT_TENSES'
                   OR code LIKE 'V55_%' OR code = 'V60_DATA_TREND_WORDS'
                """, Map.of());

        assertThat(points).hasSize(13);
        assertThat(points).allSatisfy(point -> {
            assertThat(point.get("skill")).isEqualTo("ALL");
            assertThat(point.get("status")).isEqualTo("INACTIVE");
            assertThat(point.get("learning_type")).isNull();
        });
        assertThat(points).filteredOn(point -> ((String) point.get("kind")).equals("GRAMMAR")).hasSize(8);
        assertThat(points).filteredOn(point -> ((String) point.get("kind")).equals("VOCABULARY")).hasSize(5);

        var cefrByCode = jdbc.queryForList("""
                SELECT code, cefr_level FROM knowledge_points
                WHERE code LIKE 'G55_%' OR code = 'G60_PERFECT_TENSES'
                   OR code LIKE 'V55_%' OR code = 'V60_DATA_TREND_WORDS'
                """, Map.of()).stream().collect(java.util.stream.Collectors.toMap(
                row -> (String) row.get("code"), row -> (String) row.get("cefr_level")));
        assertThat(cefrByCode).containsEntry("G60_PERFECT_TENSES", "B2")
                .containsEntry("V60_DATA_TREND_WORDS", "B2");
        assertThat(cefrByCode.entrySet()).filteredOn(entry -> !entry.getKey().equals("G60_PERFECT_TENSES")
                && !entry.getKey().equals("V60_DATA_TREND_WORDS")).allSatisfy(entry ->
                assertThat(entry.getValue()).isEqualTo("B1"));
    }

    @Test
    void connectsEachNewPointToItsStrategyAndKeepsTheWholeGraphAcyclic() {
        var edges = jdbc.queryForList("""
                SELECT prerequisite.code AS prerequisite_code, target.code AS target_code,
                       target.kind AS target_kind, target.status AS target_status
                FROM knowledge_point_prerequisites edge
                JOIN knowledge_points prerequisite ON prerequisite.id = edge.prerequisite_id
                JOIN knowledge_points target ON target.id = edge.knowledge_point_id
                WHERE prerequisite.code LIKE 'G55_%' OR prerequisite.code = 'G60_PERFECT_TENSES'
                   OR prerequisite.code LIKE 'V55_%' OR prerequisite.code = 'V60_DATA_TREND_WORDS'
                """, Map.of());

        assertThat(edges).hasSize(13);
        assertThat(edges).allSatisfy(edge -> {
            assertThat(edge.get("target_kind")).isEqualTo("STRATEGY");
            assertThat(edge.get("target_status")).isEqualTo("ACTIVE");
        });
        Set<String> expected = Set.of(
                "G55_COMPLEX_SENTENCES->W70_COMPLEX_STRUCTURES",
                "G55_RELATIVE_CLAUSES->W70_IDEA_DEVELOPMENT",
                "G55_PASSIVE_VOICE->W60_T1_KEY_FEATURES",
                "G55_COMPARISON->W60_T1_KEY_FEATURES",
                "G60_PERFECT_TENSES->W60_T1_KEY_FEATURES",
                "G55_ARTICLES->W65_DISCUSS_BOTH_VIEWS",
                "G55_SUBJECT_VERB_AGREEMENT->W65_DISCUSS_BOTH_VIEWS",
                "G55_LINKING_WORDS->W60_PARAGRAPH_COHESION",
                "V60_DATA_TREND_WORDS->W60_T1_KEY_FEATURES",
                "V55_ACADEMIC_COLLOCATIONS->W70_LEXICAL_PRECISION",
                "V55_PARAPHRASE_WORDS->W70_LEXICAL_PRECISION",
                "V55_WORD_FORMS->W70_LEXICAL_PRECISION",
                "V55_COMMON_TOPICS->W70_IDEA_DEVELOPMENT");
        assertThat(edges).extracting(edge -> edge.get("prerequisite_code") + "->" + edge.get("target_code"))
                .containsExactlyInAnyOrderElementsOf(expected);

        Integer cycles = jdbc.getJdbcTemplate().queryForObject("""
                WITH RECURSIVE walk(start_id, current_id, path, is_cycle) AS (
                    SELECT prerequisite_id, knowledge_point_id,
                           ARRAY[prerequisite_id, knowledge_point_id],
                           prerequisite_id = knowledge_point_id
                    FROM knowledge_point_prerequisites
                    UNION ALL
                    SELECT walk.start_id, edge.knowledge_point_id,
                           walk.path || edge.knowledge_point_id,
                           edge.knowledge_point_id = ANY(walk.path)
                    FROM walk
                    JOIN knowledge_point_prerequisites edge ON edge.prerequisite_id = walk.current_id
                    WHERE NOT walk.is_cycle
                )
                SELECT count(*) FROM walk WHERE is_cycle
                """, Integer.class);
        assertThat(cycles).isZero();
    }
}
