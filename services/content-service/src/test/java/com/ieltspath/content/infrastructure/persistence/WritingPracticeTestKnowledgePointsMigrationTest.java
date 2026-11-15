package com.ieltspath.content.infrastructure.persistence;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** After every migration, the 24 prompts of the Writing practice tests carry the knowledge points chosen for their type. */
@Testcontainers(disabledWithoutDocker = true)
class WritingPracticeTestKnowledgePointsMigrationTest {
    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    private static final String CHART = "DEMO_READING_W1_CHART";
    private static final String TREND = "DEMO_WRITING_TREND_LANGUAGE";
    private static final String TOPIC = "DEMO_WRITING_TOPIC_SENTENCE";
    private static final String OPINION = "TR_W_OPINION";
    private static final String BOTH_VIEWS = "W65_DISCUSS_BOTH_VIEWS";
    private static final String IDEAS = "W70_IDEA_DEVELOPMENT";
    private static final String LINKING = "DEMO_WRITING_LINKING";
    private static final String FEATURES = "W60_T1_KEY_FEATURES";

    /** Package and part, then the codes expected on that prompt in alphabetical order. */
    private static final Map<String, List<String>> EXPECTED = new TreeMap<>();
    static {
        EXPECTED.put("PT-WRITING-01/1", List.of(CHART, TREND));
        EXPECTED.put("PT-WRITING-01/2", List.of(OPINION, TOPIC));
        EXPECTED.put("PT-WRITING-02/1", List.of(CHART));
        EXPECTED.put("PT-WRITING-02/2", List.of(BOTH_VIEWS, TOPIC));
        EXPECTED.put("PT-WRITING-03/1", List.of(CHART, TREND));
        EXPECTED.put("PT-WRITING-03/2", List.of(IDEAS, TOPIC));
        EXPECTED.put("PT-WRITING-04/1", List.of(CHART, TREND));
        EXPECTED.put("PT-WRITING-04/2", List.of(OPINION, TOPIC));
        EXPECTED.put("PT-WRITING-05/1", List.of(LINKING));
        EXPECTED.put("PT-WRITING-05/2", List.of(IDEAS, TOPIC));
        EXPECTED.put("PT-WRITING-06/1", List.of(FEATURES));
        EXPECTED.put("PT-WRITING-06/2", List.of(OPINION, TOPIC));
        EXPECTED.put("PT-WRITING-07/1", List.of(CHART, TREND));
        EXPECTED.put("PT-WRITING-07/2", List.of(OPINION, TOPIC));
        EXPECTED.put("PT-WRITING-08/1", List.of(CHART, TREND));
        EXPECTED.put("PT-WRITING-08/2", List.of(BOTH_VIEWS, TOPIC));
        EXPECTED.put("PT-WRITING-09/1", List.of(LINKING));
        EXPECTED.put("PT-WRITING-09/2", List.of(OPINION, TOPIC));
        EXPECTED.put("PT-WRITING-10/1", List.of(FEATURES));
        EXPECTED.put("PT-WRITING-10/2", List.of(IDEAS, TOPIC));
        EXPECTED.put("PT-WRITING-11/1", List.of(CHART, TREND));
        EXPECTED.put("PT-WRITING-11/2", List.of(IDEAS, TOPIC));
        EXPECTED.put("PT-WRITING-12/1", List.of(CHART, TREND));
        EXPECTED.put("PT-WRITING-12/2", List.of(OPINION, TOPIC));
        // The query lists the codes of a prompt in alphabetical order.
        EXPECTED.replaceAll((prompt, codes) -> codes.stream().sorted().toList());
    }

    @BeforeAll
    static void migrate() {
        Flyway.configure().dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration").load().migrate();
    }

    private static final String LINKS = """
            SELECT p.code || '/' || cs.part_no, kp.code
            FROM content_packages p
            JOIN content_sections cs ON cs.package_version_id = p.current_published_version_id
            JOIN section_questions sq ON sq.section_id = cs.id
            JOIN question_knowledge_points q ON q.question_version_id = sq.question_version_id
            JOIN knowledge_points kp ON kp.id = q.knowledge_point_id
            WHERE p.code ~ '^PT-WRITING-(0[1-9]|1[0-2])$'
            ORDER BY 1, 2
            """;

    private static Map<String, List<String>> linked(Connection connection) throws Exception {
        Map<String, List<String>> found = new TreeMap<>();
        try (Statement statement = connection.createStatement(); ResultSet rows = statement.executeQuery(LINKS)) {
            while (rows.next()) found.computeIfAbsent(rows.getString(1), key -> new ArrayList<>()).add(rows.getString(2));
        }
        return found;
    }

    @Test
    void everyPromptCarriesTheKnowledgePointsChosenForItsType() throws Exception {
        try (Connection connection = connection()) {
            assertEquals(EXPECTED, linked(connection));
        }
    }

    @Test
    void everyLinkWeighsOneAndPointsAtAnActiveWritingPoint() throws Exception {
        try (Connection connection = connection()) {
            assertEquals("43", scalar(connection, """
                    SELECT count(*)::text FROM question_knowledge_points q
                    JOIN knowledge_points kp ON kp.id = q.knowledge_point_id
                    JOIN topics t ON t.id = kp.topic_id JOIN courses c ON c.id = t.course_id
                    WHERE q.question_version_id::text LIKE '2d000000-0000-4000-8000-04000000%'
                      AND q.weight = 1.0 AND kp.status = 'ACTIVE' AND kp.skill = 'WRITING'
                      AND t.status = 'ACTIVE' AND c.status = 'ACTIVE'
                    """));
        }
    }

    @Test
    void theseTestsBelongToNoCourseMilestone() throws Exception {
        try (Connection connection = connection()) {
            assertEquals("0", scalar(connection, """
                    SELECT count(*)::text FROM course_practice_test_milestones m
                    JOIN content_packages p ON p.id = m.package_id
                    WHERE p.code ~ '^PT-WRITING-(0[1-9]|1[0-2])$'
                    """));
        }
    }

    @Test
    void runningTheMigrationAgainOnMigratedDataChangesNothing() throws Exception {
        String sql;
        try (var input = getClass().getResourceAsStream(
                "/db/migration/V134__writing_practice_test_prompt_knowledge_points.sql")) {
            assertTrue(input != null, "the migration file is on the classpath");
            sql = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            statement.execute(sql);
            assertEquals(EXPECTED, linked(connection));
        }
    }

    private static Connection connection() throws Exception {
        return DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    }

    private static String scalar(Connection connection, String sql) throws Exception {
        try (Statement statement = connection.createStatement(); ResultSet rows = statement.executeQuery(sql)) {
            return rows.next() ? rows.getString(1) : null;
        }
    }
}
