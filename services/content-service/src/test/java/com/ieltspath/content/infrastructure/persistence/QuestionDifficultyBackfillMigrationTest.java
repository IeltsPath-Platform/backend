package com.ieltspath.content.infrastructure.persistence;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
class QuestionDifficultyBackfillMigrationTest {
    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");
    private static final UUID ORPHAN_VERSION = UUID.fromString("39000000-0000-4000-8000-000000000002");
    private static final UUID PRESET_VERSION = UUID.fromString("39000000-0000-4000-8000-000000000004");
    private static NamedParameterJdbcTemplate jdbc;
    private static long totalBefore;
    private static long nullBefore;

    @BeforeAll
    static void migrate() {
        var source = new PGSimpleDataSource();
        source.setURL(POSTGRES.getJdbcUrl());
        source.setUser(POSTGRES.getUsername());
        source.setPassword(POSTGRES.getPassword());
        Flyway.configure().dataSource(source).locations("classpath:db/migration").target("41").load().migrate();
        jdbc = new NamedParameterJdbcTemplate(source);

        // One version outside any course or package, and one whose author already chose a difficulty.
        insertVersion("39000000-0000-4000-8000-000000000001", ORPHAN_VERSION, null);
        insertVersion("39000000-0000-4000-8000-000000000003", PRESET_VERSION, "HARD");
        totalBefore = count("SELECT count(*) FROM question_versions");
        nullBefore = count("SELECT count(*) FROM question_versions WHERE difficulty IS NULL");

        Flyway.configure().dataSource(source).locations("classpath:db/migration").target("42").load().migrate();
    }

    @Test
    void migrationSucceedsAndLeavesOnlyTheUnclassifiableVersionNull() {
        assertThat(nullBefore).isGreaterThan(1);
        assertThat(count("SELECT count(*) FROM question_versions")).isEqualTo(totalBefore);
        // Questions of library practice tests (those with a source) belong to no course, so they stay untagged too.
        var stillNull = jdbc.queryForList("""
                SELECT qv.id FROM question_versions qv
                WHERE qv.difficulty IS NULL AND NOT EXISTS (
                    SELECT 1 FROM section_questions sq
                    JOIN content_sections cs ON cs.id = sq.section_id
                    JOIN content_package_versions v ON v.id = cs.package_version_id
                    JOIN content_packages p ON p.id = v.package_id
                    WHERE sq.question_version_id = qv.id AND p.package_type = 'PRACTICE_TEST'
                      AND p.source_code IS NOT NULL)
                """, Map.of());
        assertThat(stillNull).extracting(row -> row.get("id")).containsExactly(ORPHAN_VERSION);
    }

    @Test
    void keepsAnAuthorChosenDifficulty() {
        String difficulty = jdbc.queryForObject("SELECT difficulty FROM question_versions WHERE id = :id",
                Map.of("id", PRESET_VERSION), String.class);
        assertThat(difficulty).isEqualTo("HARD");
    }

    @Test
    void onlyUsesEasyAndMediumForClassifiedVersions() {
        var values = jdbc.queryForList("SELECT DISTINCT difficulty FROM question_versions "
                + "WHERE difficulty IS NOT NULL AND id <> :preset", Map.of("preset", PRESET_VERSION));
        assertThat(values).extracting(row -> row.get("difficulty")).containsExactlyInAnyOrder("EASY", "MEDIUM");
    }

    @Test
    void course65QuestionsAreMediumAndCourse55QuestionsAreEasy() {
        String bandCheck = "SELECT count(DISTINCT qv.id) FROM question_versions qv "
                + "JOIN question_knowledge_points qkp ON qkp.question_version_id = qv.id "
                + "JOIN knowledge_points kp ON kp.id = qkp.knowledge_point_id "
                + "JOIN topics t ON t.id = kp.topic_id JOIN courses c ON c.id = t.course_id "
                + "WHERE c.band_level = %s AND qv.difficulty <> '%s'";
        assertThat(count(String.format(bandCheck, "6.5", "MEDIUM"))).isZero();
        // Course 5.5 questions are EASY unless they are also placement questions (MEDIUM).
        assertThat(count(String.format(bandCheck, "5.5", "EASY")
                + " AND qv.id NOT IN (SELECT sq.question_version_id FROM section_questions sq "
                + "JOIN content_sections cs ON cs.id = sq.section_id "
                + "JOIN content_package_versions v ON v.id = cs.package_version_id "
                + "JOIN content_packages p ON p.id = v.package_id WHERE p.package_type = 'PLACEMENT_TEST')"))
                .isZero();
    }

    private static void insertVersion(String questionId, UUID versionId, String difficulty) {
        var question = new HashMap<String, Object>();
        question.put("q", UUID.fromString(questionId));
        jdbc.update("INSERT INTO questions (id, question_type, skill, status) "
                + "VALUES (:q, 'MULTIPLE_CHOICE', 'READING', 'DRAFT')", question);
        var version = new HashMap<String, Object>();
        version.put("v", versionId);
        version.put("q", UUID.fromString(questionId));
        version.put("d", difficulty);
        jdbc.update("INSERT INTO question_versions (id, question_id, version_number, stem, difficulty) "
                + "VALUES (:v, :q, 1, 'Backfill fixture', :d)", version);
    }

    private static long count(String sql) {
        return jdbc.queryForObject(sql, Map.of(), Long.class);
    }
}
