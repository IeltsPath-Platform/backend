package com.ieltspath.content.infrastructure.persistence;

import com.ieltspath.content.domain.service.KnowledgeGraph;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
class KnowledgeTreeDraftSeedMigrationTest {
    private static final Set<String> NEW_CODES = Set.of(
            "R60_SKIM_STRUCTURE", "R60_SUMMARY_COMPLETION", "L60_FORM_NOTE_COMPLETION", "L60_SIGNPOSTING",
            "W60_PARAGRAPH_COHESION", "W60_T1_KEY_FEATURES", "R70_WRITER_VIEW_YNNG", "R70_HEDGING_SCOPE",
            "R70_COMPLEX_PARAPHRASE", "L70_MULTI_SPEAKER_ATTITUDE", "L70_MAP_DIAGRAM", "W70_IDEA_DEVELOPMENT",
            "W70_LEXICAL_PRECISION", "W70_COMPLEX_STRUCTURES");
    private static final List<String[]> EDGES = List.of(
            edge("TR_R_SCANNING", "PR_SENTENCE_COMPLETION"), edge("PR_SENTENCE_COMPLETION", "R60_SUMMARY_COMPLETION"),
            edge("DR_TOPIC_SENTENCE", "DR_MATCHING_HEADINGS"), edge("DR_MATCHING_HEADINGS", "R60_SKIM_STRUCTURE"),
            edge("TR_R_WORD_IN_CONTEXT", "R65_PARAPHRASE"), edge("R65_PARAPHRASE", "R70_COMPLEX_PARAPHRASE"),
            edge("TFNG_FALSE_VS_NOT_GIVEN", "R65_EVIDENCE_INFERENCE"),
            edge("R65_EVIDENCE_INFERENCE", "R70_WRITER_VIEW_YNNG"), edge("R65_EVIDENCE_INFERENCE", "R70_HEDGING_SCOPE"),
            edge("LS_NUM", "L60_FORM_NOTE_COMPLETION"), edge("LS_SPELL", "L60_FORM_NOTE_COMPLETION"),
            edge("L60_FORM_NOTE_COMPLETION", "L65_CORRECTION"), edge("LS_PARA", "LS_TRAP"), edge("LS_TRAP", "L65_CORRECTION"),
            edge("TR_L_DETAIL", "L60_SIGNPOSTING"), edge("L60_SIGNPOSTING", "L65_SPEAKER_PURPOSE"),
            edge("L65_SPEAKER_PURPOSE", "L70_MULTI_SPEAKER_ATTITUDE"), edge("L60_SIGNPOSTING", "L70_MAP_DIAGRAM"),
            edge("TR_W_OPINION", "W60_PARAGRAPH_COHESION"), edge("W60_PARAGRAPH_COHESION", "W65_DISCUSS_BOTH_VIEWS"),
            edge("W65_DISCUSS_BOTH_VIEWS", "W70_IDEA_DEVELOPMENT"), edge("DEMO_READING_W1_CHART", "W60_T1_KEY_FEATURES"),
            edge("W60_PARAGRAPH_COHESION", "W70_COMPLEX_STRUCTURES"), edge("TR_R_WORD_IN_CONTEXT", "W70_LEXICAL_PRECISION"));

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");
    private static NamedParameterJdbcTemplate jdbc;
    private static List<Map<String, Object>> oldKnowledgePointsBefore;

    @BeforeAll
    static void migrate() {
        var source = new PGSimpleDataSource();
        source.setURL(POSTGRES.getJdbcUrl());
        source.setUser(POSTGRES.getUsername());
        source.setPassword(POSTGRES.getPassword());
        Flyway.configure().dataSource(source).locations("classpath:db/migration").target("42").load().migrate();
        jdbc = new NamedParameterJdbcTemplate(source);
        oldKnowledgePointsBefore = jdbc.queryForList(
                "SELECT id, code, topic_id, status FROM knowledge_points ORDER BY code", Map.of());
        Flyway.configure().dataSource(source).locations("classpath:db/migration").target("43").load().migrate();
    }

    @Test
    void addsExactlyTheFourteenNewKnowledgePointsInTheRightCourseAndSkill() {
        var rows = jdbc.queryForList("SELECT kp.code, kp.skill, kp.status, kp.learning_type, t.skill AS topic_skill, "
                + "c.band_level, c.status AS course_status FROM knowledge_points kp "
                + "JOIN topics t ON t.id = kp.topic_id JOIN courses c ON c.id = t.course_id "
                + "WHERE c.code IN ('IELTS_6_0', 'IELTS_7_0')", Map.of());
        assertThat(rows).extracting(row -> row.get("code")).containsExactlyInAnyOrderElementsOf(NEW_CODES);
        for (var row : rows) {
            String code = (String) row.get("code");
            assertThat(code).matches("^[RLW](55|60|65|70)_[A-Z0-9_]{2,60}$");
            assertThat(row.get("skill")).isEqualTo(row.get("topic_skill"));
            assertThat(code.substring(0, 1)).isEqualTo(((String) row.get("skill")).substring(0, 1));
            assertThat(row.get("status")).isEqualTo("ACTIVE");
            assertThat(row.get("learning_type")).isNotNull();
            assertThat(row.get("course_status")).isEqualTo("INACTIVE");
            String band = ((java.math.BigDecimal) row.get("band_level")).toPlainString();
            assertThat(code.substring(1, 3)).isEqualTo(band.replace(".", "").substring(0, 2));
        }
    }

    @Test
    void keepsEveryOlderKnowledgePointUnchanged() {
        var after = jdbc.queryForList("SELECT id, code, topic_id, status FROM knowledge_points "
                + "WHERE code NOT IN (:codes) ORDER BY code", Map.of("codes", NEW_CODES));
        assertThat(after).isEqualTo(oldKnowledgePointsBefore);
    }

    @Test
    void seedsEveryDraftEdge() {
        var stored = jdbc.queryForList("SELECT p.code AS prerequisite, k.code AS target "
                + "FROM knowledge_point_prerequisites e JOIN knowledge_points p ON p.id = e.prerequisite_id "
                + "JOIN knowledge_points k ON k.id = e.knowledge_point_id", Map.of());
        var pairs = stored.stream().map(row -> row.get("prerequisite") + ">" + row.get("target"))
                .collect(Collectors.toSet());
        var expected = EDGES.stream().map(e -> e[0] + ">" + e[1]).collect(Collectors.toSet());
        assertThat(pairs).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    void treeIsAcyclic() {
        var edges = jdbc.queryForList("SELECT prerequisite_id, knowledge_point_id FROM knowledge_point_prerequisites",
                Map.of()).stream()
                .map(row -> new KnowledgeGraph.Edge((UUID) row.get("prerequisite_id"),
                        (UUID) row.get("knowledge_point_id")))
                .toList();
        new KnowledgeGraph().ensureAcyclic(edges);
    }

    @Test
    void treeCoversFourBandsAndThreeSkills() {
        var bands = jdbc.queryForList("SELECT DISTINCT c.band_level FROM knowledge_points kp "
                + "JOIN topics t ON t.id = kp.topic_id JOIN courses c ON c.id = t.course_id "
                + "WHERE kp.status = 'ACTIVE'", Map.of());
        assertThat(bands).extracting(row -> ((java.math.BigDecimal) row.get("band_level")).toPlainString())
                .contains("5.5", "6.0", "6.5", "7.0");
        var skills = jdbc.queryForList("SELECT DISTINCT kp.skill FROM knowledge_points kp "
                + "JOIN topics t ON t.id = kp.topic_id JOIN courses c ON c.id = t.course_id "
                + "WHERE kp.status = 'ACTIVE' AND c.band_level IN (6.0, 7.0)", Map.of());
        assertThat(skills).extracting(row -> row.get("skill")).containsExactlyInAnyOrder("READING", "LISTENING", "WRITING");
    }

    private static String[] edge(String prerequisite, String target) {
        return new String[] {prerequisite, target};
    }
}
