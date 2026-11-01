package com.ieltspath.content.infrastructure.persistence;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.content.application.port.PracticeTestCatalogReader;
import com.ieltspath.content.application.result.PracticeTestCardResult;
import com.ieltspath.content.domain.vo.Skill;
import com.ieltspath.content.infrastructure.persistence.adapter.JdbcPracticeTestCatalogReader;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/** The full-length Listening practice tests (04 and later): four parts of ten questions, a recording and a cover photo for each part. */
@Testcontainers(disabledWithoutDocker = true)
class ListeningFullLengthTestsMigrationTest {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String CODES = "'PT-LISTENING-04','PT-LISTENING-05','PT-LISTENING-06','PT-LISTENING-07','PT-LISTENING-08'";

    private static final String QUESTIONS = "SELECT p.code AS code, s.part_no AS part_no, g.sort_order AS group_no, g.format_code AS format_code, "
            + "sq.sort_order AS position, qv.stem AS stem, qv.options::text AS options, qv.answer_spec::text AS answer_spec "
            + "FROM content_sections s JOIN content_packages p ON p.current_published_version_id = s.package_version_id "
            + "JOIN section_question_groups g ON g.section_id = s.id JOIN section_questions sq ON sq.group_id = g.id "
            + "JOIN question_versions qv ON qv.id = sq.question_version_id "
            + "WHERE p.code IN (" + CODES + ") ORDER BY p.code, s.part_no, g.sort_order, sq.sort_order";

    private static final String RECORDINGS = "SELECT p.code AS code, s.part_no AS part_no, a.media_reference AS media_reference, "
            + "a.duration_seconds AS duration_seconds, a.text_content AS transcript "
            + "FROM content_sections s JOIN content_packages p ON p.current_published_version_id = s.package_version_id "
            + "JOIN content_asset_links l ON l.section_id = s.id JOIN content_assets a ON a.id = l.asset_id "
            + "WHERE p.code IN (" + CODES + ") AND a.asset_type = 'AUDIO' ORDER BY p.code, s.part_no";

    private static final String COVERS = "SELECT p.code AS code, s.part_no AS part_no, c.id AS cover_id, c.asset_type AS asset_type, "
            + "c.media_reference AS media_reference, c.validation_status AS validation_status "
            + "FROM content_sections s JOIN content_packages p ON p.current_published_version_id = s.package_version_id "
            + "LEFT JOIN content_assets c ON c.id = s.cover_asset_id "
            + "WHERE p.code IN (" + CODES + ") ORDER BY p.code, s.part_no";

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");
    private static NamedParameterJdbcTemplate jdbc;
    private static List<Map<String, Object>> questions;
    private static List<Map<String, Object>> recordings;
    private static List<Map<String, Object>> covers;

    @BeforeAll
    static void migrate() {
        var source = new PGSimpleDataSource();
        source.setURL(POSTGRES.getJdbcUrl());
        source.setUser(POSTGRES.getUsername());
        source.setPassword(POSTGRES.getPassword());
        Flyway.configure().dataSource(source).locations("classpath:db/migration").load().migrate();
        jdbc = new NamedParameterJdbcTemplate(source);
        questions = jdbc.queryForList(QUESTIONS, Collections.emptyMap());
        recordings = jdbc.queryForList(RECORDINGS, Collections.emptyMap());
        covers = jdbc.queryForList(COVERS, Collections.emptyMap());
    }

    @ParameterizedTest
    @ValueSource(strings = {"PT-LISTENING-04", "PT-LISTENING-05", "PT-LISTENING-06", "PT-LISTENING-07", "PT-LISTENING-08"})
    void hasFourPartsOfTenQuestionsWithTheFormatsOfTheRealTest(String code) {
        List<Map<String, Object>> own = of(questions, code);
        assertThat(own).hasSize(40);
        for (int part = 1; part <= 4; part++) {
            int current = part;
            assertThat(own).filteredOn(row -> Integer.valueOf(current).equals(row.get("part_no"))).hasSize(10);
        }
        assertThat(formatsOfPart(own, 1)).containsExactly("GAP_FILLING");
        assertThat(formatsOfPart(own, 2)).containsExactly("MULTIPLE_CHOICE_ONE", "MATCHING");
        assertThat(formatsOfPart(own, 3)).containsExactly("MULTIPLE_CHOICE_ONE", "MATCHING");
        assertThat(formatsOfPart(own, 4)).containsExactly("GAP_FILLING");
    }

    @ParameterizedTest
    @ValueSource(strings = {"PT-LISTENING-04", "PT-LISTENING-05", "PT-LISTENING-06", "PT-LISTENING-07", "PT-LISTENING-08"})
    void everyPartHasItsRecordingOfSeveralMinutes(String code) {
        List<Map<String, Object>> own = of(recordings, code);
        assertThat(own).hasSize(4);
        for (int part = 1; part <= 4; part++) {
            Map<String, Object> recording = own.get(part - 1);
            assertThat(recording.get("media_reference")).isEqualTo("listening/practice/" + code.toLowerCase() + "-s" + part + ".mp3");
            assertThat(((Number) recording.get("duration_seconds")).intValue()).isBetween(150, 600);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"PT-LISTENING-04", "PT-LISTENING-05", "PT-LISTENING-06", "PT-LISTENING-07", "PT-LISTENING-08"})
    void everyGapAnswerIsSpokenInTheRecordingOfItsPart(String code) throws Exception {
        List<Map<String, Object>> recordingsOfTest = of(recordings, code);
        for (Map<String, Object> row : of(questions, code)) {
            JsonNode spec = JSON.readTree((String) row.get("answer_spec"));
            if (!"FILL".equals(spec.get("type").asText())) continue;
            String transcript = normalise((String) recordingsOfTest.get((Integer) row.get("part_no") - 1).get("transcript"));
            List<String> accepted = JSON.convertValue(spec.get("accepted"), JSON.getTypeFactory().constructCollectionType(List.class, String.class));
            assertThat(accepted).as(row.get("stem").toString()).isNotEmpty();
            assertThat(accepted.stream().map(ListeningFullLengthTestsMigrationTest::normalise))
                    .as("an accepted answer of \"%s\" is in the script", row.get("stem")).anyMatch(transcript::contains);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"PT-LISTENING-04", "PT-LISTENING-05", "PT-LISTENING-06", "PT-LISTENING-07", "PT-LISTENING-08"})
    void everyChoiceAnswerIsOneOfItsOptions(String code) throws Exception {
        for (Map<String, Object> row : of(questions, code)) {
            JsonNode spec = JSON.readTree((String) row.get("answer_spec"));
            if (!"CHOICE".equals(spec.get("type").asText())) continue;
            JsonNode options = JSON.readTree((String) row.get("options"));
            List<String> keys = new ArrayList<>();
            options.forEach(option -> keys.add(option.get("optionKey").asText()));
            assertThat(keys).as(row.get("stem").toString()).contains(spec.get("correct").asText());
            assertThat(keys).first().isEqualTo("A");
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"PT-LISTENING-04", "PT-LISTENING-05", "PT-LISTENING-06", "PT-LISTENING-07", "PT-LISTENING-08"})
    void everyPartHasAPhotoCoverNamedAfterItsAsset(String code) {
        List<Map<String, Object>> own = of(covers, code);
        assertThat(own).hasSize(4).allSatisfy(cover -> {
            assertThat(cover.get("asset_type")).isEqualTo("IMAGE");
            assertThat(cover.get("validation_status")).isEqualTo("VALID");
            assertThat(cover.get("media_reference")).isEqualTo("practice/covers/" + cover.get("cover_id") + ".webp");
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"PT-LISTENING-04", "PT-LISTENING-05", "PT-LISTENING-06", "PT-LISTENING-07", "PT-LISTENING-08"})
    void theCatalogOffersItAsAFullTestOfFortyQuestionsWithACover(String code) {
        var catalog = new JdbcPracticeTestCatalogReader(jdbc);
        PracticeTestCatalogReader.Page full = catalog.search(new PracticeTestCatalogReader.Search(Skill.LISTENING, "FULL", null,
                List.of(), null, null, null, 0, 50));

        PracticeTestCardResult card = full.items().stream().filter(item -> code.equals(item.packageCode())).findFirst().orElseThrow();
        assertThat(card.questionCount()).isEqualTo(40);
        assertThat(card.parts()).hasSize(4);
        assertThat(card.formats()).contains("GAP_FILLING", "MULTIPLE_CHOICE_ONE", "MATCHING");
        assertThat(card.coverReference()).startsWith("practice/covers/").endsWith(".webp");
    }

    private static List<Map<String, Object>> of(List<Map<String, Object>> rows, String code) {
        return rows.stream().filter(row -> code.equals(row.get("code"))).collect(Collectors.toList());
    }

    private static List<String> formatsOfPart(List<Map<String, Object>> rows, int part) {
        return rows.stream().filter(row -> Integer.valueOf(part).equals(row.get("part_no")))
                .map(row -> (String) row.get("format_code")).distinct().collect(Collectors.toList());
    }

    /** Lower case, no punctuation, single spaces: "Nine five eight, four two one." and "nine five eight four two one" compare equal. */
    private static String normalise(String text) {
        return text.toLowerCase().replaceAll("[^a-z0-9£.:\\s]", " ").replaceAll("(?<![0-9])[.:]|[.:](?![0-9])", " ").replaceAll("\\s+", " ").trim();
    }
}
