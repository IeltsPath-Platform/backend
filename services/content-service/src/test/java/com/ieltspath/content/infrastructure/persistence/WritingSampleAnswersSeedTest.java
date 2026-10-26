package com.ieltspath.content.infrastructure.persistence;

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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/** The model answers of the course Writing tasks: one paragraph per part, long enough, with well-formed highlights. */
@Testcontainers(disabledWithoutDocker = true)
class WritingSampleAnswersSeedTest {
    private static final List<String> TASK_CODES = List.of(
            "PS-W1-WR-1", "PS-W1-WR-2", "PS-W1-WR-3", "PS-W2-WR-1", "PS-W2-WR-2", "PS-W3-WR-1",
            "PS-W3-WR-2", "PS-W4-WR-1", "PS-W4-WR-2", "PS-W5-WR-1", "PS-W5-WR-2");
    /** The same rule as the grader: letters and digits, joined by an apostrophe or a hyphen, count as one word. */
    private static final Pattern WORD = Pattern.compile("[\\p{L}\\p{N}]+(?:['’-][\\p{L}\\p{N}]+)*");
    private static final Pattern HIGHLIGHT = Pattern.compile("\\*\\*[^*]+\\*\\*");

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    private static NamedParameterJdbcTemplate jdbc;

    @BeforeAll
    static void migrate() {
        PGSimpleDataSource dataSource = new PGSimpleDataSource();
        dataSource.setUrl(POSTGRES.getJdbcUrl());
        dataSource.setUser(POSTGRES.getUsername());
        dataSource.setPassword(POSTGRES.getPassword());
        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").load().migrate();
        jdbc = new NamedParameterJdbcTemplate(dataSource);
    }

    private static List<Map<String, Object>> samples() {
        return jdbc.queryForList("""
                SELECT p.code, qv.explanation, qv.answer_spec ->> 'task' AS task,
                       (qv.answer_spec ->> 'minWords')::int AS min_words
                FROM content_packages p
                JOIN content_sections s ON s.package_version_id = p.current_published_version_id
                JOIN section_questions sq ON sq.section_id = s.id
                JOIN question_versions qv ON qv.id = sq.question_version_id
                WHERE p.code IN (:codes) AND qv.answer_spec ->> 'type' = 'ESSAY'
                ORDER BY p.code
                """, Map.of("codes", TASK_CODES));
    }

    private static int words(String text) {
        Matcher matcher = WORD.matcher(text.replace("**", ""));
        int count = 0;
        while (matcher.find()) count++;
        return count;
    }

    @Test
    void everyCourseWritingTaskHasASample() {
        assertThat(samples()).extracting(row -> row.get("code")).containsExactlyElementsOf(TASK_CODES);
    }

    @Test
    void eachSampleHasFourParagraphsAndMeetsItsMinimumLength() {
        for (Map<String, Object> row : samples()) {
            String code = (String) row.get("code");
            String sample = (String) row.get("explanation");
            assertThat(sample.strip().split("\\n\\s*\\n")).as("%s paragraphs", code).hasSize(4);
            assertThat(words(sample)).as("%s words", code).isGreaterThanOrEqualTo((Integer) row.get("min_words"));
        }
    }

    @Test
    void highlightsAreWellFormedAndFewEnoughToStandOut() {
        for (Map<String, Object> row : samples()) {
            String code = (String) row.get("code");
            String sample = (String) row.get("explanation");
            Matcher highlights = HIGHLIGHT.matcher(sample);
            int count = 0;
            while (highlights.find()) count++;
            assertThat(count).as("%s highlighted phrases", code).isBetween(4, 8);
            assertThat(HIGHLIGHT.matcher(sample).replaceAll("")).as("%s stray asterisks", code).doesNotContain("*");
        }
    }

    @Test
    void samplesAreEnglishOnly() {
        for (Map<String, Object> row : samples()) {
            assertThat((String) row.get("explanation")).as("%s", row.get("code")).matches("(?s)[\\x00-\\x7F]+");
        }
    }
}
