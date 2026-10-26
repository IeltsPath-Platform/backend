package com.ieltspath.content.infrastructure.persistence;

import com.ieltspath.content.infrastructure.persistence.adapter.JdbcLearningContentReader;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
class PracticeSetCoverageMigrationTest {
    private static final String SEEDED_PACKAGES = "p.code IN (SELECT code FROM content_packages WHERE code LIKE 'PS-R65-%' "
            + "OR code LIKE 'PS-TRW-OP-%' OR code LIKE 'PS-W2-OP-%' OR code LIKE 'PS-W65-DBV-%' OR code LIKE 'PS-W1-CH-%')";
    /** Listening sets need new audio files, so these knowledge points are still short of three sets. */
    private static final Set<String> KNOWN_LISTENING_GAP = Set.of(
            "LS_NUM", "LS_SPELL", "LS_PARA", "LS_TRAP", "TR_L_DETAIL", "L65_CORRECTION", "L65_SPEAKER_PURPOSE");
    private static final ObjectMapper JSON = new ObjectMapper();

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");
    private static NamedParameterJdbcTemplate jdbc;
    private static JdbcLearningContentReader reader;

    @BeforeAll
    static void migrate() {
        var source = new PGSimpleDataSource();
        source.setURL(POSTGRES.getJdbcUrl());
        source.setUser(POSTGRES.getUsername());
        source.setPassword(POSTGRES.getPassword());
        Flyway.configure().dataSource(source).locations("classpath:db/migration").target("79").load().migrate();
        jdbc = new NamedParameterJdbcTemplate(source);
        reader = new JdbcLearningContentReader(jdbc);
    }

    @Test
    void everyReadingAndWritingKnowledgePointOfCourses55And65HasThreeEligibleSets() {
        var counts = eligibleSetCountsByCode();
        var short_ = counts.entrySet().stream().filter(e -> e.getValue() < 3).map(Map.Entry::getKey)
                .collect(Collectors.toSet());
        short_.removeAll(KNOWN_LISTENING_GAP);
        assertThat(short_).as("non-listening knowledge points with fewer than 3 eligible practice sets").isEmpty();
    }

    @Test
    void onlyTheListeningKnowledgePointsAreStillShort() {
        // Fails once Listening sets with new audio are added: then delete KNOWN_LISTENING_GAP and the test above
        // already enforces three sets for every knowledge point.
        var counts = eligibleSetCountsByCode();
        var short_ = counts.entrySet().stream().filter(e -> e.getValue() < 3).map(Map.Entry::getKey)
                .collect(Collectors.toSet());
        assertThat(short_).isEqualTo(KNOWN_LISTENING_GAP);
    }

    @Test
    void seededQuestionsAreUsedByExactlyOnePackageAndCarryDifficultyAndKnowledgePoint() {
        long questions = count("SELECT count(*) FROM question_versions qv JOIN section_questions sq ON sq.question_version_id = qv.id "
                + "JOIN content_sections s ON s.id = sq.section_id JOIN content_packages p ON p.current_published_version_id = s.package_version_id "
                + "WHERE " + SEEDED_PACKAGES);
        assertThat(questions).isEqualTo(23);
        assertThat(count("SELECT count(*) FROM question_versions qv JOIN section_questions sq ON sq.question_version_id = qv.id "
                + "JOIN content_sections s ON s.id = sq.section_id JOIN content_packages p ON p.current_published_version_id = s.package_version_id "
                + "WHERE " + SEEDED_PACKAGES + " AND (qv.difficulty IS NULL OR qv.status <> 'PUBLISHED' "
                + "OR NOT EXISTS (SELECT 1 FROM question_knowledge_points k WHERE k.question_version_id = qv.id))")).isZero();
        assertThat(count("SELECT count(*) FROM (SELECT sq.question_version_id FROM section_questions sq "
                + "JOIN content_sections s ON s.id = sq.section_id JOIN content_packages p ON p.current_published_version_id = s.package_version_id "
                + "WHERE " + SEEDED_PACKAGES + " GROUP BY 1 HAVING count(*) > 1) reused")).isZero();
    }

    @Test
    void eachCriterionKnowledgePointHasThreeFreeStandingSetsOfThreeQuestions() {
        var sets = jdbc.queryForList("SELECT kp.code, p.code AS package_code, p.topic_id, p.lesson_id, "
                + "count(sq.id) AS questions, "
                + "count(sq.id) FILTER (WHERE qv.status = 'PUBLISHED' AND qv.difficulty IS NOT NULL "
                + "AND qkp.knowledge_point_id = kp.id) AS valid "
                + "FROM content_packages p JOIN content_sections s ON s.package_version_id = p.current_published_version_id "
                + "JOIN section_questions sq ON sq.section_id = s.id "
                + "JOIN question_versions qv ON qv.id = sq.question_version_id "
                + "JOIN question_knowledge_points qkp ON qkp.question_version_id = qv.id "
                + "JOIN knowledge_points kp ON kp.code LIKE 'DEMO_WRITING_CRITERION_%' AND kp.id = qkp.knowledge_point_id "
                + "WHERE p.code LIKE 'PS-WCR-%' GROUP BY kp.code, p.code, p.topic_id, p.lesson_id",
                Collections.emptyMap());

        assertThat(sets).hasSize(15);
        assertThat(sets).allSatisfy(set -> {
            assertThat(set.get("topic_id")).isNull();
            assertThat(set.get("lesson_id")).isNull();
            assertThat(set.get("questions")).isEqualTo(3L);
            assertThat(set.get("valid")).isEqualTo(3L);
        });
        assertThat(sets.stream().collect(Collectors.groupingBy(set -> (String) set.get("code"), Collectors.counting())))
                .containsOnlyKeys("DEMO_WRITING_CRITERION_TA", "DEMO_WRITING_CRITERION_TR", "DEMO_WRITING_CRITERION_CC",
                        "DEMO_WRITING_CRITERION_LR", "DEMO_WRITING_CRITERION_GRA")
                .allSatisfy((code, total) -> assertThat(total).isEqualTo(3L));
    }

    @Test
    void answerSpecsMatchTheirQuestionType() throws Exception {
        var rows = jdbc.queryForList("SELECT q.question_type, qv.options::text AS options, qv.answer_spec::text AS spec "
                + "FROM question_versions qv JOIN questions q ON q.id = qv.question_id "
                + "JOIN section_questions sq ON sq.question_version_id = qv.id JOIN content_sections s ON s.id = sq.section_id "
                + "JOIN content_packages p ON p.current_published_version_id = s.package_version_id WHERE " + SEEDED_PACKAGES,
                Collections.emptyMap());
        for (var row : rows) {
            JsonNode spec = JSON.readTree((String) row.get("spec"));
            switch ((String) row.get("question_type")) {
                case "MULTIPLE_CHOICE" -> {
                    var keys = new java.util.HashSet<String>();
                    JSON.readTree((String) row.get("options")).forEach(o -> keys.add(o.get("optionKey").asText()));
                    assertThat(keys).hasSize(3).contains(spec.get("correct").asText());
                }
                case "FILL_IN_BLANK" -> assertThat(spec.get("accepted")).isNotEmpty();
                case "ESSAY" -> {
                    assertThat(spec.get("minWords").asInt()).isGreaterThanOrEqualTo(150);
                    assertThat(spec.get("task").asText()).isIn("TASK_1", "TASK_2");
                    if ("TASK_1".equals(spec.get("task").asText())) {
                        assertThat(spec.get("chartFacts").asText()).isNotBlank();
                    }
                }
                default -> throw new AssertionError("unexpected question type " + row.get("question_type"));
            }
        }
    }

    @Test
    void everyTaskOneSetCarriesAValidChartImage() {
        var uris = jdbc.queryForList("SELECT a.media_reference FROM content_assets a JOIN content_asset_links l ON l.asset_id = a.id "
                + "JOIN content_sections s ON s.id = l.section_id JOIN content_packages p ON p.current_published_version_id = s.package_version_id "
                + "WHERE p.code LIKE 'PS-W1-CH-%' AND a.asset_type = 'IMAGE'", Collections.emptyMap(), String.class);
        assertThat(uris).hasSize(3);
        for (String uri : uris) {
            assertThat(uri).startsWith("data:image/svg+xml;base64,");
            String svg = new String(Base64.getDecoder().decode(uri.substring(uri.indexOf(',') + 1)));
            assertThat(svg).startsWith("<svg").endsWith("</svg>");
        }
    }

    private Map<String, Integer> eligibleSetCountsByCode() {
        var knowledgePoints = jdbc.queryForList("SELECT kp.id, kp.code FROM knowledge_points kp JOIN topics t ON t.id = kp.topic_id "
                        + "JOIN courses c ON c.id = t.course_id WHERE kp.status = 'ACTIVE' AND c.band_level IN (5.5, 6.5) "
                        + "AND kp.id NOT IN (SELECT trap_knowledge_point_id FROM error_types WHERE trap_knowledge_point_id IS NOT NULL)",
                Collections.emptyMap());
        List<UUID> ids = knowledgePoints.stream().map(row -> (UUID) row.get("id")).toList();
        var counts = reader.countEligiblePracticeSets(ids, List.of(), 1);
        return knowledgePoints.stream().collect(Collectors.toMap(row -> (String) row.get("code"),
                row -> counts.getOrDefault((UUID) row.get("id"), 0)));
    }

    private static long count(String sql) {
        return jdbc.queryForObject(sql, Collections.emptyMap(), Long.class);
    }
}
