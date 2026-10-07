package com.ieltspath.content.infrastructure.persistence;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.*;

/** Validates the actual seed resource even when PostgreSQL integration tests cannot run. */
class CourseSeedResourceTest {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Pattern INSERT = Pattern.compile("INSERT INTO (\\w+) \\(([^)]+)\\) VALUES\\s*\\((.*?)\\);", Pattern.DOTALL);

    @Test
    void courseSeedBackfillsEverySkilledTopicBeforeAddingTheHigherBandTopic() throws Exception {
        String seed = seed();
        List<Map<String, String>> courses = rows(seed, "courses");
        assertThat(courses).extracting(row -> row.get("code")).containsExactly("IELTS_5_5", "IELTS_6_5");
        assertThat(courses).extracting(row -> row.get("band_level")).containsExactly("5.5", "6.5");
        assertThat(seed).contains("WHERE skill IS NOT NULL");
        assertThat(seed.indexOf("UPDATE topics SET course_id")).isLessThan(seed.indexOf("INSERT INTO topics"));
        assertThat(rows(seed, "topics")).singleElement().satisfies(topic -> {
            assertThat(topic.get("code")).isEqualTo("READING_6_5_INFERENCE");
            assertThat(topic.get("course_id")).isEqualTo(courses.getLast().get("id"));
            assertThat(topic.get("skill")).isEqualTo("READING");
        });
    }

    @Test
    void newQuestionsHaveGradeableSpecsMatchingTheirTypesAndOptions() throws Exception {
        String seed = seed();
        Map<String, Map<String, String>> questions = index(rows(seed, "questions"), "id");
        List<Map<String, String>> versions = rows(seed, "question_versions");
        assertThat(versions).isNotEmpty();
        assertThat(versions).extracting(version -> version.get("id")).doesNotHaveDuplicates();
        assertThat(questions).hasSize(versions.size());
        for (var version : versions) {
            var question = questions.get(version.get("question_id"));
            assertThat(question.get("skill")).isEqualTo("READING");
            assertThat(question.get("purpose")).isEqualTo("LEARNING");
            assertThat(question.get("status")).isEqualTo("PUBLISHED");
            assertThat(version.get("status")).isEqualTo("PUBLISHED");
            assertThat(version.get("stem")).isNotBlank();
            assertThat(version.get("explanation")).isNotBlank();
            JsonNode spec = JSON.readTree(version.get("answer_spec"));
            if ("FILL".equals(spec.path("type").asText())) {
                assertThat(question.get("question_type")).isEqualTo("FILL_IN_BLANK");
                assertThat(version.get("options")).isEqualTo("NULL");
                assertThat(spec.path("accepted").isArray()).isTrue();
                assertThat(spec.path("accepted").isEmpty()).isFalse();
                for (JsonNode answer : spec.path("accepted")) {
                    assertThat(answer.isTextual()).isTrue();
                    assertThat(answer.asText()).isNotBlank();
                }
            } else {
                assertThat(spec.path("type").asText()).isEqualTo("CHOICE");
                assertThat(question.get("question_type")).isEqualTo("MULTIPLE_CHOICE");
                JsonNode options = JSON.readTree(version.get("options"));
                assertThat(options.isArray()).isTrue();
                List<String> keys = new ArrayList<>();
                for (JsonNode option : options) {
                    keys.add(option.path("optionKey").asText());
                    assertThat(option.path("content").asText()).isNotBlank();
                }
                assertThat(keys).hasSizeGreaterThanOrEqualTo(3).doesNotHaveDuplicates().contains(spec.path("correct").asText());
                assertThat(spec.path("correct").isTextual()).isTrue();
            }
            assertThat(seed).contains("SET current_published_version_id = '" + version.get("id") + "'");
        }
    }

    @Test
    void courseTestsHaveSixToTenUniqueQuestionsAndPracticeHasThreePerKnowledgePoint() throws Exception {
        String seed = seed();
        var packages = rows(seed, "content_packages");
        var versions = index(rows(seed, "content_package_versions"), "package_id");
        var sections = index(rows(seed, "content_sections"), "package_version_id");
        var questionOwners = new HashMap<String, Integer>();
        var sectionQuestions = rows(seed, "section_questions");
        for (var item : sectionQuestions) questionOwners.merge(item.get("question_version_id"), 1, Integer::sum);
        for (var item : rows(seed, "lesson_block_questions")) questionOwners.merge(item.get("question_version_id"), 1, Integer::sum);
        assertThat(questionOwners.values()).allMatch(count -> count == 1);
        assertThat(questionOwners.keySet()).containsExactlyInAnyOrderElementsOf(
                rows(seed, "question_versions").stream().map(row -> row.get("id")).toList());
        assertThat(packages.stream().filter(pkg -> "COURSE_TEST".equals(pkg.get("package_type"))).toList()).hasSize(2);
        assertThat(packages.stream().filter(pkg -> "PRACTICE_SET".equals(pkg.get("package_type"))).toList()).hasSize(2);
        for (var pkg : packages) {
            var version = versions.get(pkg.get("id"));
            var section = sections.get(version.get("id"));
            assertThat(pkg.get("status")).isEqualTo("PUBLISHED");
            assertThat(version.get("status")).isEqualTo("PUBLISHED");
            assertThat(section.get("skill")).isEqualTo("READING");
            long count = sectionQuestions.stream().filter(item -> section.get("id").equals(item.get("section_id"))).count();
            if ("COURSE_TEST".equals(pkg.get("package_type"))) {
                assertThat(count).isBetween(6L, 10L);
                assertThat(pkg.get("course_id")).isNotEqualTo("NULL");
            } else if ("PRACTICE_SET".equals(pkg.get("package_type"))) {
                assertThat(count).isGreaterThanOrEqualTo(3);
                assertThat(pkg.get("lesson_id")).isEqualTo(rows(seed, "lessons").getFirst().get("id"));
            }
        }
        assertThat(rows(seed, "knowledge_points")).hasSize(2);
        assertThat(rows(seed, "lesson_block_knowledge_points")).hasSize(2);
    }

    private static String seed() throws Exception {
        try (var stream = CourseSeedResourceTest.class.getResourceAsStream("/db/migration/V21__seed_courses.sql")) {
            assertThat(stream).as("Course seed migration resource").isNotNull();
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static List<Map<String, String>> rows(String seed, String table) {
        List<Map<String, String>> rows = new ArrayList<>();
        var inserts = INSERT.matcher(seed);
        while (inserts.find()) {
            if (!table.equals(inserts.group(1))) continue;
            var columns = inserts.group(2).split(",\\s*");
            var values = values(inserts.group(3));
            assertThat(values).as("Values of " + table).hasSize(columns.length);
            Map<String, String> row = new LinkedHashMap<>();
            for (int i = 0; i < columns.length; i++) row.put(columns[i].trim(), values.get(i));
            rows.add(row);
        }
        return rows;
    }

    private static List<String> values(String tuple) {
        List<String> values = new ArrayList<>();
        int start = 0;
        boolean quoted = false;
        for (int i = 0; i < tuple.length(); i++) {
            char c = tuple.charAt(i);
            if (c == '\'') {
                if (quoted && i + 1 < tuple.length() && tuple.charAt(i + 1) == '\'') i++;
                else quoted = !quoted;
            } else if (c == ',' && !quoted) {
                values.add(value(tuple.substring(start, i)));
                start = i + 1;
            }
        }
        values.add(value(tuple.substring(start)));
        return values;
    }

    private static String value(String token) {
        String value = token.trim().replaceFirst("::jsonb$", "");
        return value.startsWith("'") ? value.substring(1, value.length() - 1).replace("''", "'") : value;
    }

    private static Map<String, Map<String, String>> index(List<Map<String, String>> rows, String key) {
        Map<String, Map<String, String>> index = new LinkedHashMap<>();
        for (var row : rows) assertThat(index.put(row.get(key), row)).as("Unique " + key).isNull();
        return index;
    }
}
