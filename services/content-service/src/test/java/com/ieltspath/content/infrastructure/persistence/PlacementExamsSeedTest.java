package com.ieltspath.content.infrastructure.persistence;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.content.application.result.PackageVersionContentResult;
import com.ieltspath.content.application.result.PlacementPackageResult;
import com.ieltspath.content.domain.vo.PlacementPool;
import com.ieltspath.content.domain.vo.QuestionPurpose;
import com.ieltspath.content.domain.vo.Skill;
import com.ieltspath.content.infrastructure.persistence.adapter.JdbcLearningContentReader;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/** The placement exams seeded per pool: shape, pool, publication, answer specs, ownership of questions and audio keys. */
@Testcontainers(disabledWithoutDocker = true)
class PlacementExamsSeedTest {
    @Container private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");
    private static final ObjectMapper JSON = new ObjectMapper();

    private static final Map<String, PlacementPool> NEW_EXAMS = Map.of(
            "PLACEMENT-B03-A", PlacementPool.BAND_0_3, "PLACEMENT-B03-B", PlacementPool.BAND_0_3,
            "PLACEMENT-B35-A", PlacementPool.BAND_3_5, "PLACEMENT-B35-B", PlacementPool.BAND_3_5,
            "PLACEMENT-B59-B", PlacementPool.BAND_5_9);

    private static NamedParameterJdbcTemplate jdbc;
    private static JdbcLearningContentReader reader;

    @BeforeAll
    static void migrate() {
        var source = new PGSimpleDataSource();
        source.setURL(POSTGRES.getJdbcUrl());
        source.setUser(POSTGRES.getUsername());
        source.setPassword(POSTGRES.getPassword());
        Flyway.configure().dataSource(source).locations("classpath:db/migration").load().migrate();
        jdbc = new NamedParameterJdbcTemplate(source);
        reader = new JdbcLearningContentReader(jdbc);
    }

    @Test
    void everyPoolOpensWithTwoPublishedExams() {
        List<PlacementPackageResult> all = reader.placementTestPackages(null);
        assertThat(all).extracting(PlacementPackageResult::code).containsExactlyInAnyOrder(
                "PLACEMENT-4SKILLS", "PLACEMENT-B03-A", "PLACEMENT-B03-B", "PLACEMENT-B35-A", "PLACEMENT-B35-B",
                "PLACEMENT-B59-B");
        Map<PlacementPool, Long> perPool = all.stream().collect(
                Collectors.groupingBy(PlacementPackageResult::pool, Collectors.counting()));
        assertThat(perPool).containsEntry(PlacementPool.BAND_0_3, 2L).containsEntry(PlacementPool.BAND_3_5, 2L)
                .containsEntry(PlacementPool.BAND_5_9, 2L);
        for (PlacementPool pool : PlacementPool.values()) {
            assertThat(reader.placementTestPackages(pool)).hasSize(2).allSatisfy(item -> assertThat(item.pool()).isEqualTo(pool));
        }
    }

    @Test
    void eachPackageIsPublishedInThePoolOfItsCode() {
        NEW_EXAMS.forEach((code, pool) -> {
            Map<String, Object> row = jdbc.queryForMap(
                    "SELECT p.placement_pool, p.status, p.package_type, v.status AS version_status, v.version_number, "
                            + "p.current_published_version_id = v.id AS current_is_version FROM content_packages p "
                            + "JOIN content_package_versions v ON v.package_id = p.id WHERE p.code = :code", Map.of("code", code));
            assertThat(row).as(code).containsEntry("placement_pool", pool.name()).containsEntry("status", "PUBLISHED")
                    .containsEntry("package_type", "PLACEMENT_TEST").containsEntry("version_status", "PUBLISHED")
                    .containsEntry("version_number", 1).containsEntry("current_is_version", true);
        });
    }

    @Test
    void questionCountsMatchTheShapeOfEachExam() {
        NEW_EXAMS.forEach((code, pool) -> {
            Map<String, Integer> counts = jdbc.query(
                    "SELECT s.skill, count(*) AS n FROM content_packages p JOIN content_package_versions v ON v.package_id = p.id "
                            + "JOIN content_sections s ON s.package_version_id = v.id JOIN section_questions sq ON sq.section_id = s.id "
                            + "WHERE p.code = :code GROUP BY s.skill", Map.of("code", code), rs -> {
                        Map<String, Integer> result = new java.util.HashMap<>();
                        while (rs.next()) result.put(rs.getString("skill"), rs.getInt("n"));
                        return result;
                    });
            boolean full = pool == PlacementPool.BAND_5_9;
            assertThat(counts).as(code).containsEntry("LISTENING", full ? 40 : 20).containsEntry("READING", full ? 40 : 20)
                    .containsEntry("WRITING", full ? 2 : 1).containsEntry("SPEAKING", full ? 8 : 2);
        });
    }

    @Test
    void everyQuestionIsPublishedPlacementContentWithAValidAnswerSpec() throws Exception {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT p.code, q.id AS question_id, q.question_type, q.skill, q.purpose, q.status, "
                        + "q.current_published_version_id = qv.id AS current_is_version, qv.status AS version_status, "
                        + "qv.explanation, qv.stem, qv.options::text AS options, qv.answer_spec::text AS answer_spec "
                        + "FROM content_packages p JOIN content_package_versions v ON v.package_id = p.id "
                        + "JOIN content_sections s ON s.package_version_id = v.id JOIN section_questions sq ON sq.section_id = s.id "
                        + "JOIN question_versions qv ON qv.id = sq.question_version_id JOIN questions q ON q.id = qv.question_id "
                        + "WHERE p.code IN (:codes)", Map.of("codes", NEW_EXAMS.keySet()));
        assertThat(rows).hasSize(4 * 43 + 90);
        for (Map<String, Object> row : rows) {
            assertThat(row).containsEntry("purpose", "PLACEMENT").containsEntry("status", "PUBLISHED")
                    .containsEntry("version_status", "PUBLISHED").containsEntry("current_is_version", true);
            assertThat((String) row.get("stem")).isNotBlank();
            assertThat((String) row.get("explanation")).isNotBlank();
            assertAnswerSpec(row);
        }
    }

    @Test
    void noQuestionIsSharedAndEveryPackagePassesThePublishOwnershipRules() {
        for (String code : NEW_EXAMS.keySet()) {
            UUID version = reader.placementTestPackages(null).stream().filter(item -> item.code().equals(code))
                    .findFirst().orElseThrow().packageVersionId();
            assertThat(reader.questionsUsedElsewhere(version)).as(code).isEmpty();
            assertThat(reader.questionsWithWrongPurpose(version, QuestionPurpose.PLACEMENT)).as(code).isEmpty();
        }
        // A question of a new exam may appear in no other package version (the original exam reuses its own two
        // questions between its versions; those are not part of this check).
        assertThat(jdbc.queryForList(
                "SELECT sq.question_version_id FROM section_questions sq JOIN content_sections s ON s.id = sq.section_id "
                        + "WHERE sq.question_version_id IN (SELECT sq2.question_version_id FROM section_questions sq2 "
                        + "JOIN content_sections s2 ON s2.id = sq2.section_id "
                        + "JOIN content_package_versions v ON v.id = s2.package_version_id "
                        + "JOIN content_packages p ON p.id = v.package_id WHERE p.code IN (:codes)) "
                        + "GROUP BY sq.question_version_id HAVING count(DISTINCT s.package_version_id) > 1",
                Map.of("codes", NEW_EXAMS.keySet()), UUID.class)).isEmpty();
    }

    @Test
    void listeningPartsCarryOneRecordingEachUnderTheKeyOfTheirPackage() {
        Set<String> keys = new HashSet<>();
        NEW_EXAMS.keySet().forEach(code -> {
            PackageVersionContentResult version = reader.publishedPackageVersion(reader.placementTestPackages(null).stream()
                    .filter(item -> item.code().equals(code)).findFirst().orElseThrow().packageVersionId()).orElseThrow();
            List<PackageVersionContentResult.Section> listening = version.sections().stream()
                    .filter(section -> section.skill() == Skill.LISTENING).toList();
            assertThat(listening).as(code).hasSize(code.equals("PLACEMENT-B59-B") ? 4 : 2);
            for (int index = 0; index < listening.size(); index++) {
                var audio = listening.get(index).audio();
                assertThat(audio).as(code).isNotNull();
                assertThat(audio.mediaReference()).isEqualTo("listening/placement/" + code.toLowerCase() + "/part" + (index + 1) + ".mp3");
                assertThat(audio.durationSeconds()).isPositive();
                assertThat(audio.transcript()).isNotBlank();
                keys.add(audio.mediaReference());
            }
        });
        assertThat(keys).hasSize(12);
    }

    @Test
    void sectionsFollowTheOrderOfTheTestAndEveryPartHasItsMaterial() {
        NEW_EXAMS.keySet().forEach(code -> {
            PackageVersionContentResult version = reader.publishedPackageVersion(reader.placementTestPackages(null).stream()
                    .filter(item -> item.code().equals(code)).findFirst().orElseThrow().packageVersionId()).orElseThrow();
            assertThat(version.sections()).extracting(PackageVersionContentResult.Section::sortOrder)
                    .containsExactlyElementsOf(java.util.stream.IntStream.rangeClosed(1, version.sections().size()).boxed().toList());
            List<Skill> order = version.sections().stream().map(PackageVersionContentResult.Section::skill).distinct().toList();
            assertThat(order).as(code).containsExactly(Skill.LISTENING, Skill.READING, Skill.WRITING, Skill.SPEAKING);
            version.sections().stream().filter(section -> section.skill() == Skill.READING)
                    .forEach(section -> assertThat(section.passage()).as(code).isNotBlank());
            version.sections().stream().filter(section -> section.skill() == Skill.WRITING).forEach(section -> {
                boolean task1 = section.items().getFirst().answerSpecJson().contains("\"TASK_1\"");
                if (task1) {
                    assertThat(section.passage()).as(code + " Task 1 table").contains(" | ");
                    assertThat(section.items().getFirst().answerSpecJson()).contains("chartFacts");
                }
            });
        });
    }

    private static void assertAnswerSpec(Map<String, Object> row) throws Exception {
        String type = (String) row.get("question_type");
        String skill = (String) row.get("skill");
        JsonNode spec = JSON.readTree((String) row.get("answer_spec"));
        String code = row.get("code") + " / " + row.get("stem");
        switch (type) {
            case "FILL_IN_BLANK" -> {
                assertThat(spec.path("type").asText()).as(code).isEqualTo("FILL");
                assertThat(spec.path("accepted").isArray() && !spec.path("accepted").isEmpty()).as(code).isTrue();
                spec.path("accepted").forEach(value -> assertThat(value.isTextual() && !value.asText().isBlank()).as(code).isTrue());
                assertThat(row.get("options")).isNull();
            }
            case "MULTIPLE_CHOICE", "TRUE_FALSE_NOT_GIVEN" -> {
                assertThat(spec.path("type").asText()).as(code).isEqualTo("CHOICE");
                JsonNode options = JSON.readTree((String) row.get("options"));
                Set<String> keys = new HashSet<>();
                options.forEach(option -> keys.add(option.path("optionKey").asText()));
                assertThat(keys).as(code).hasSize(options.size()).contains(spec.path("correct").asText());
            }
            case "ESSAY" -> {
                assertThat(skill).isEqualTo("WRITING");
                assertThat(spec.path("type").asText()).as(code).isEqualTo("ESSAY");
                assertThat(spec.path("task").asText()).as(code).isIn("TASK_1", "TASK_2");
                assertThat(spec.path("minWords").asInt()).as(code).isPositive();
                double band = spec.path("passBand").asDouble(-1);
                assertThat(band >= 0 && band <= 9 && band % 0.5 == 0).as(code).isTrue();
                if ("TASK_1".equals(spec.path("task").asText())) assertThat(spec.path("chartFacts").asText()).as(code).isNotBlank();
            }
            case "SPEAKING" -> {
                assertThat(skill).isEqualTo("SPEAKING");
                assertThat(spec.path("type").asText()).as(code).isEqualTo("SPEAKING");
                assertThat(spec.path("durationSeconds").asInt()).as(code).isPositive();
            }
            default -> throw new AssertionError("Unexpected question type " + type);
        }
    }
}
