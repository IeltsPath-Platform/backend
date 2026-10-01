package com.group01.content.infrastructure.persistence;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.group01.content.application.result.LessonContentResult;
import com.group01.content.application.result.LessonSummaryResult;
import com.group01.content.application.result.PackageVersionContentResult;
import com.group01.content.application.result.PracticeSetResult;
import com.group01.content.application.result.TopicSequenceResult;
import com.group01.content.domain.vo.BlockType;
import com.group01.content.domain.vo.PackageType;
import com.group01.content.infrastructure.persistence.adapter.JdbcLearningContentReader;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Lesson schema, demo seed and the learning-content queries against a real PostgreSQL migrated to the latest. */
@Testcontainers(disabledWithoutDocker = true)
class LessonPipelineSeedTest {
    private static final UUID DEMO_READING = UUID.fromString("10000000-0000-4000-8000-000000000001");
    private static final ObjectMapper JSON = new ObjectMapper();

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    private static PGSimpleDataSource dataSource;
    private static NamedParameterJdbcTemplate jdbc;
    private static JdbcLearningContentReader reader;

    @BeforeAll
    static void migrate() {
        dataSource = new PGSimpleDataSource();
        dataSource.setUrl(POSTGRES.getJdbcUrl());
        dataSource.setUser(POSTGRES.getUsername());
        dataSource.setPassword(POSTGRES.getPassword());
        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").load().migrate();
        jdbc = new NamedParameterJdbcTemplate(dataSource);
        reader = new JdbcLearningContentReader(jdbc);
    }

    // ---- schema ----

    @Test
    void topicTestPackagesMustBelongToATopic() {
        assertThrows(SQLException.class, () -> execute(
                "INSERT INTO content_packages (code, title, package_type) VALUES ('X-NO-TOPIC', 'x', 'TOPIC_TEST')"));
        int topicTest = rollbackAfter(c -> update(c, "INSERT INTO content_packages (code, title, package_type, "
                + "topic_id) VALUES ('X-TOPIC', 'x', 'TOPIC_TEST', '" + DEMO_READING + "')"));
        int lessonPackage = rollbackAfter(c -> update(c,
                "INSERT INTO content_packages (code, title, package_type) VALUES ('LESSON-PKG', 'x', 'LESSON')"));
        assertThat(topicTest).isEqualTo(1);
        assertThat(lessonPackage).isEqualTo(1);
    }

    @Test
    void lessonOrderIsUniqueWithinATopic() {
        assertThrows(SQLException.class, () -> execute("INSERT INTO lessons (topic_id, code, title, sort_order) "
                + "VALUES ('" + DEMO_READING + "', 'L1-DUP', 'Duplicate', 1)"));
    }

    // ---- seed ----

    @Test
    void seedMatchesTheMvpSeedContent() throws Exception {
        JsonNode expected;
        try (InputStream in = getClass().getResourceAsStream("/seed/lesson-pipeline-demo-expected.json")) {
            expected = JSON.readTree(in);
        }
        assertThat(expected).hasSize(43);
        for (JsonNode item : expected) {
            UUID versionId = UUID.fromString(item.get("questionVersionId").asText());
            Map<String, Object> row = jdbc.queryForMap("""
                    SELECT qv.stem, qv.options IS NULL AS fill, qv.answer_spec::text AS spec, kp.code AS kp
                    FROM question_versions qv
                    JOIN questions q ON q.current_published_version_id = qv.id AND q.status = 'PUBLISHED'
                    JOIN question_knowledge_points qkp ON qkp.question_version_id = qv.id
                    JOIN knowledge_points kp ON kp.id = qkp.knowledge_point_id
                    WHERE qv.id = :id AND qv.status = 'PUBLISHED'
                    """, Map.of("id", versionId));
            String code = item.get("code").asText();
            assertThat(row.get("stem")).as(code).isEqualTo(item.get("stem").asText());
            assertThat(row.get("kp")).as(code).isEqualTo(item.get("knowledgePointCode").asText());
            assertThat(row.get("fill")).as(code).isEqualTo(item.get("fill").asBoolean());
            JsonNode spec = JSON.readTree((String) row.get("spec"));
            assertThat(grade(spec, item.get("correctResponse").asText())).as(code + " correct answer").isTrue();
            assertThat(grade(spec, "certainly wrong")).as(code + " wrong answer").isFalse();
            assertThat(containerOf(versionId)).as(code).contains(item.get("container").asText());
        }
    }

    @Test
    void everySeedAnswerSpecIsAValidV1Spec() throws Exception {
        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT qv.options::text AS options, qv.answer_spec::text AS spec FROM question_versions qv
                WHERE qv.id::text LIKE '20000000-%'
                """, Map.of());
        assertThat(rows).hasSize(43);
        for (Map<String, Object> row : rows) {
            JsonNode spec = JSON.readTree((String) row.get("spec"));
            String type = spec.path("type").asText();
            if (row.get("options") == null) {
                assertThat(type).isEqualTo("FILL");
                assertThat(spec.path("accepted").size()).isPositive();
            } else {
                assertThat(type).isEqualTo("CHOICE");
                List<String> keys = new ArrayList<>();
                JSON.readTree((String) row.get("options")).forEach(o -> keys.add(o.get("optionKey").asText()));
                assertThat(keys).contains(spec.get("correct").asText());
            }
        }
    }

    @Test
    void practiceSetQuestionsAreNeverLessonOrTestQuestions() {
        Integer overlap = jdbc.queryForObject("""
                SELECT count(*) FROM section_questions sq
                JOIN content_sections s ON s.id = sq.section_id
                JOIN content_package_versions v ON v.id = s.package_version_id
                JOIN content_packages p ON p.id = v.package_id
                WHERE p.package_type = 'PRACTICE_SET'
                  AND (sq.question_version_id IN (SELECT question_version_id FROM lesson_block_questions)
                       OR sq.question_version_id IN (
                           SELECT tsq.question_version_id FROM section_questions tsq
                           JOIN content_sections ts ON ts.id = tsq.section_id
                           JOIN content_package_versions tv ON tv.id = ts.package_version_id
                           JOIN content_packages tp ON tp.id = tv.package_id
                           WHERE tp.package_type = 'TOPIC_TEST'))
                """, Map.of(), Integer.class);
        assertThat(overlap).isZero();
    }

    // ---- reads ----

    @Test
    void topicSequenceListsTopicsWithLessonsAndTestsAndTheirActiveKnowledgePoints() {
        List<TopicSequenceResult> sequence = reader.topicSequence(3);

        assertThat(sequence).extracting(TopicSequenceResult::code).containsExactly("DEMO_READING", "TFNG_SKILLS");
        assertThat(sequence.get(0).knowledgePoints()).extracting(TopicSequenceResult.KnowledgePointEntry::code)
                .containsExactly("DEMO_READING_MAIN_IDEA", "DR_IDEA_OR_DETAIL", "DR_TOPIC_SENTENCE",
                        "DR_MATCHING_HEADINGS");
        assertThat(sequence.get(0).knowledgePoints()).allMatch(TopicSequenceResult.KnowledgePointEntry::hasPracticeSet);
        assertThat(sequence.get(1).knowledgePoints()).singleElement()
                .satisfies(kp -> {
                    assertThat(kp.code()).isEqualTo("TFNG_FALSE_VS_NOT_GIVEN");
                    assertThat(kp.hasPracticeSet()).isFalse();
                });
    }

    @Test
    void aKnowledgePointWhoseOnlyPracticeSetIsTooSmallHasNoPracticeSet() {
        // KP5 gets one extra one-question practice set; it stays below the default minimum of three questions.
        Boolean hasPracticeSet = rollbackAfter(connection -> {
            update(connection, """
                    INSERT INTO content_packages (id, code, title, package_type, status)
                    VALUES ('90000000-0000-4000-8000-000000000001', 'PS-TINY', 'Tiny', 'PRACTICE_SET', 'PUBLISHED');
                    INSERT INTO content_package_versions (id, package_id, version_number, status)
                    VALUES ('90000000-0000-4000-8000-000000000002', '90000000-0000-4000-8000-000000000001', 1, 'PUBLISHED');
                    UPDATE content_packages SET current_published_version_id = '90000000-0000-4000-8000-000000000002'
                    WHERE id = '90000000-0000-4000-8000-000000000001';
                    INSERT INTO content_sections (id, package_version_id, title, skill, sort_order)
                    VALUES ('90000000-0000-4000-8000-000000000003', '90000000-0000-4000-8000-000000000002', 'S', 'READING', 1);
                    INSERT INTO questions (id, question_type, skill, status)
                    VALUES ('90000000-0000-4000-8000-000000000004', 'MULTIPLE_CHOICE', 'READING', 'PUBLISHED');
                    INSERT INTO question_versions (id, question_id, version_number, stem, answer_spec, status)
                    VALUES ('90000000-0000-4000-8000-000000000005', '90000000-0000-4000-8000-000000000004', 1, 'S',
                            '{"type":"CHOICE","correct":"A"}', 'PUBLISHED');
                    INSERT INTO question_knowledge_points (question_version_id, knowledge_point_id)
                    SELECT '90000000-0000-4000-8000-000000000005', id FROM knowledge_points
                    WHERE code = 'TFNG_FALSE_VS_NOT_GIVEN';
                    INSERT INTO section_questions (section_id, question_version_id, sort_order)
                    VALUES ('90000000-0000-4000-8000-000000000003', '90000000-0000-4000-8000-000000000005', 1);
                    """);
            JdbcLearningContentReader sameTransaction = readerOn(connection);
            return sameTransaction.topicSequence(3).get(1).knowledgePoints().get(0).hasPracticeSet()
                    || !sameTransaction.searchPracticeSets(kpId("TFNG_FALSE_VS_NOT_GIVEN"), List.of(), 3, 10).isEmpty();
        });
        assertThat(hasPracticeSet).isFalse();
    }

    @Test
    void practiceSetSearchHonoursExclusionsAndSkipsTheOneQuestionDemoPackage() {
        UUID kp2 = kpId("DR_IDEA_OR_DETAIL");
        PracticeSetResult first = reader.searchPracticeSets(kp2, List.of(), 3, 1).get(0);
        PracticeSetResult second = reader.searchPracticeSets(kp2, List.of(first.packageId()), 3, 1).get(0);
        assertThat(Set.of(first.code(), second.code())).containsExactlyInAnyOrder("PS-KP2-A", "PS-KP2-B");
        assertThat(reader.searchPracticeSets(kp2, List.of(first.packageId(), second.packageId()), 3, 1)).isEmpty();
        assertThat(first.questionCount()).isEqualTo(4);
        assertThat(first.matchedQuestionCount()).isEqualTo(4);

        List<PracticeSetResult> kp1 = reader.searchPracticeSets(kpId("DEMO_READING_MAIN_IDEA"), List.of(), 3, 10);
        assertThat(kp1).extracting(PracticeSetResult::code).containsExactly("PS-KP1-A", "PS-KP1-B");
    }

    @Test
    void topicLessonsAreInOrderWithTheirKnowledgePointsAndExerciseBlocks() {
        List<LessonSummaryResult> lessons = reader.publishedLessons(DEMO_READING);

        assertThat(lessons).extracting(LessonSummaryResult::code).containsExactly("L1", "L2", "L3", "L4");
        assertThat(lessons.get(0).exerciseBlockIds()).hasSize(2);
        assertThat(lessons.get(3).knowledgePointIds())
                .containsExactly(kpId("DEMO_READING_MAIN_IDEA"), kpId("DR_MATCHING_HEADINGS"));
    }

    @Test
    void lessonContentHasBlocksInOrderWithFullQuestions() {
        UUID l1 = jdbc.queryForObject("SELECT id FROM lessons WHERE code = 'L1'", Map.of(), UUID.class);
        LessonContentResult lesson = reader.publishedLesson(l1).orElseThrow();

        assertThat(lesson.blocks()).extracting(LessonContentResult.Block::blockType)
                .containsExactly(BlockType.TEXT, BlockType.ASSET, BlockType.EXERCISE, BlockType.TEXT,
                        BlockType.EXERCISE);
        assertThat(lesson.blocks().get(1).asset().textContent()).startsWith("A. Across Europe and North America");
        LessonContentResult.Block lastExercise = lesson.blocks().get(4);
        assertThat(lastExercise.questions()).extracting(LessonContentResult.Question::sortOrder)
                .containsExactly(1, 2, 3);
        LessonContentResult.Question fill = lastExercise.questions().get(2);
        assertThat(fill.optionsJson()).isNull();
        assertThat(fill.answerSpecJson()).contains("critics");
        assertThat(fill.knowledgePointIds()).containsExactly(kpId("DR_TOPIC_SENTENCE"));
        assertThat(lesson.knowledgePointIds()).containsExactly(kpId("DR_TOPIC_SENTENCE"));
        assertThat(reader.publishedLesson(UUID.randomUUID())).isEmpty();
    }

    @Test
    void topicTestPackagesAndTheirVersionContent() {
        assertThat(reader.publishedTestPackages(DEMO_READING)).extracting(p -> p.code())
                .containsExactlyInAnyOrder("X1", "X2");

        UUID x1Version = reader.publishedTestPackages(DEMO_READING).stream()
                .filter(p -> p.code().equals("X1")).findFirst().orElseThrow().packageVersionId();
        PackageVersionContentResult x1 = reader.publishedPackageVersion(x1Version).orElseThrow();

        assertThat(x1.packageType()).isEqualTo(PackageType.TOPIC_TEST);
        assertThat(x1.topicId()).isEqualTo(DEMO_READING);
        assertThat(x1.rulesJson()).isEqualTo("{}");
        assertThat(x1.sections()).singleElement().satisfies(section -> {
            assertThat(section.passage()).startsWith("A. City trees");
            assertThat(section.items()).hasSize(4);
            assertThat(section.items()).allSatisfy(item -> {
                assertThat(item.maxScore()).isEqualTo(1.0);
                assertThat(item.knowledgePointMappings()).hasSize(1);
            });
        });
        assertThat(reader.publishedPackageVersion(UUID.randomUUID())).isEmpty();
    }

    @Test
    void lessonPracticeAndTestQuestionsAreReservedForLearning() {
        List<UUID> all = jdbc.queryForList("SELECT id FROM question_versions", Map.of(), UUID.class);
        Set<UUID> reserved = reader.questionVersionsReservedForLearning(all);
        // 43 seeded questions plus the V4 practice-set question.
        assertThat(reserved).hasSize(44);
        assertThat(reader.questionVersionsReservedForLearning(List.of(UUID.randomUUID()))).isEmpty();
    }

    // ---- helpers ----

    /** Answer spec v1 grading of one response, for checking the seed. */
    private static boolean grade(JsonNode spec, String response) {
        String type = spec.path("type").asText("CHOICE");
        if (type.equals("CHOICE")) {
            return spec.get("correct").asText().equals(response);
        }
        String normalized = normalize(response);
        for (JsonNode accepted : spec.get("accepted")) {
            if (normalize(accepted.asText()).equals(normalized)) {
                return true;
            }
        }
        return false;
    }

    private static String normalize(String value) {
        return value.strip().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    private static List<String> containerOf(UUID questionVersionId) {
        return jdbc.queryForList("""
                SELECT l.code FROM lesson_block_questions bq
                JOIN lesson_blocks b ON b.id = bq.block_id JOIN lessons l ON l.id = b.lesson_id
                WHERE bq.question_version_id = :id
                UNION
                SELECT p.code FROM section_questions sq
                JOIN content_sections s ON s.id = sq.section_id
                JOIN content_package_versions v ON v.id = s.package_version_id
                JOIN content_packages p ON p.id = v.package_id
                WHERE sq.question_version_id = :id
                """, Map.of("id", questionVersionId), String.class);
    }

    private static UUID kpId(String code) {
        return jdbc.queryForObject("SELECT id FROM knowledge_points WHERE code = :code", Map.of("code", code),
                UUID.class);
    }

    private static JdbcLearningContentReader readerOn(Connection connection) {
        return new JdbcLearningContentReader(
                new NamedParameterJdbcTemplate(new SingleConnectionDataSource(connection, true)));
    }

    private static void execute(String sql) throws SQLException {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        }
    }

    private static int update(Connection connection, String sql) {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
            return Math.max(statement.getUpdateCount(), 0);
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    private interface InTransaction<T> {
        T run(Connection connection) throws Exception;
    }

    /** Runs work on one connection and rolls it back, so the shared seeded database stays unchanged. */
    private static <T> T rollbackAfter(InTransaction<T> work) {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                return work.run(connection);
            } finally {
                connection.rollback();
            }
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
