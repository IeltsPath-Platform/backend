package com.ieltspath.content.infrastructure.persistence;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.content.application.result.LessonContentResult;
import com.ieltspath.content.application.result.LessonPracticeSetResult;
import com.ieltspath.content.application.result.LessonPracticeSetsResult;
import com.ieltspath.content.application.result.LessonSummaryResult;
import com.ieltspath.content.application.result.PackageVersionContentResult;
import com.ieltspath.content.application.result.PracticeSetResult;
import com.ieltspath.content.application.result.TopicSequenceResult;
import com.ieltspath.content.application.usecase.GetPackageVersionContentUseCase;
import com.ieltspath.content.domain.vo.MediaReferencePolicy;
import com.ieltspath.content.domain.vo.PackageType;
import com.ieltspath.content.domain.vo.QuestionUsageConflict;
import com.ieltspath.content.domain.vo.QuestionPurpose;
import com.ieltspath.content.domain.vo.Skill;
import com.ieltspath.content.infrastructure.persistence.adapter.JdbcLearningContentReader;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.dao.DataAccessException;
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
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Lesson schema, demo seed and the learning-content queries against a real PostgreSQL migrated to the latest. */
@Testcontainers(disabledWithoutDocker = true)
class LessonPipelineSeedTest {
    private static final UUID DEMO_READING = UUID.fromString("10000000-0000-4000-8000-000000000001");
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final MediaReferencePolicy MEDIA = new MediaReferencePolicy("https://media.example.test/ieltspath");

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
        }
    }

    @Test
    void everySeedAnswerSpecIsAValidV1Spec() throws Exception {
        assertValidV1Specs("20000000-%", 43);
    }

    @Test
    void premiumTopicSeedAnswerSpecsAreValidV1Specs() throws Exception {
        assertValidV1Specs("24000000-%", 10);
    }

    private static void assertValidV1Specs(String idPattern, int expectedCount) throws Exception {
        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT qv.options::text AS options, qv.answer_spec::text AS spec FROM question_versions qv
                WHERE qv.id::text LIKE :idPattern
                """, Map.of("idPattern", idPattern));
        assertThat(rows).hasSize(expectedCount);
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
    void seededQuestionsHaveOneOwnerAndMockTestsDetectPackageQuestions() {
        List<Map<String, Object>> conflicts = jdbc.queryForList("""
                WITH owners AS (
                    SELECT DISTINCT qv.question_id, 'PACKAGE' AS owner_type, p.id AS owner_id, p.code AS owner_code
                    FROM section_questions sq
                    JOIN question_versions qv ON qv.id = sq.question_version_id
                    JOIN content_sections s ON s.id = sq.section_id
                    JOIN content_package_versions v ON v.id = s.package_version_id
                    JOIN content_packages p ON p.id = v.package_id
                    WHERE v.status = 'PUBLISHED'
                      AND p.package_type IN ('PRACTICE_SET', 'TOPIC_TEST', 'MOCK_TEST', 'PLACEMENT_TEST')
                )
                SELECT question_id, string_agg(owner_type || ' ' || owner_code, ', '
                                               ORDER BY owner_type, owner_code) AS owners
                FROM owners
                GROUP BY question_id HAVING count(*) > 1
                ORDER BY question_id
                """, Map.of());
        assertThat(conflicts).as("Seed questions with multiple owners").isEmpty();

        rollbackAfter(connection -> {
            update(connection, """
                    INSERT INTO content_packages (id, code, title, package_type, status)
                    VALUES ('90000000-0000-4000-8000-000000000001', 'MOCK-LESSON', 'Mock', 'MOCK_TEST', 'PUBLISHED');
                    INSERT INTO content_package_versions (id, package_id, version_number, status)
                    VALUES ('90000000-0000-4000-8000-000000000002', '90000000-0000-4000-8000-000000000001', 1, 'PUBLISHED');
                    UPDATE content_packages SET current_published_version_id = '90000000-0000-4000-8000-000000000002'
                    WHERE id = '90000000-0000-4000-8000-000000000001';
                    INSERT INTO content_sections (id, package_version_id, title, skill, sort_order)
                    VALUES ('90000000-0000-4000-8000-000000000003', '90000000-0000-4000-8000-000000000002', 'S', 'READING', 1);
                    INSERT INTO section_questions (section_id, question_version_id, sort_order)
                    SELECT '90000000-0000-4000-8000-000000000003', sq.question_version_id, 1
                    FROM section_questions sq
                    JOIN content_sections s ON s.id = sq.section_id
                    JOIN content_package_versions v ON v.id = s.package_version_id
                    JOIN content_packages p ON p.id = v.package_id
                    WHERE p.code = 'PS-KP1-A' ORDER BY sq.sort_order LIMIT 1;
                    """);
            UUID mockVersion = UUID.fromString("90000000-0000-4000-8000-000000000002");
            JdbcLearningContentReader sameTransaction = readerOn(connection);
            assertThat(sameTransaction.questionsWithWrongPurpose(mockVersion, QuestionPurpose.EXAM)).hasSize(1);
            assertThat(sameTransaction.questionsUsedElsewhere(mockVersion)).singleElement().satisfies(conflict -> {
                assertThat(conflict.ownerType()).isEqualTo(QuestionUsageConflict.OwnerType.PACKAGE);
                assertThat(conflict.ownerCode()).isEqualTo("PS-KP1-A");
            });
            UUID kp1 = kpId("DEMO_READING_MAIN_IDEA");
            for (String type : List.of("MOCK_TEST", "PLACEMENT_TEST")) {
                update(connection, "UPDATE content_packages SET package_type = '" + type + "' WHERE code = 'MOCK-LESSON'");
                assertThat(sameTransaction.searchPracticeSets(kp1, List.of(), 3, 10, null))
                        .extracting(PracticeSetResult::code).doesNotContain("PS-KP1-A");
                assertThat(sameTransaction.countEligiblePracticeSets(List.of(kp1), List.of(), 3).get(kp1)).isEqualTo(3);
            }
            update(connection, """
                    INSERT INTO questions (id, question_type, skill, purpose)
                    VALUES ('90000000-0000-4000-8000-000000000004', 'MULTIPLE_CHOICE', 'READING', 'EXAM');
                    INSERT INTO question_versions (id, question_id, version_number, stem, answer_spec)
                    VALUES ('90000000-0000-4000-8000-000000000005', '90000000-0000-4000-8000-000000000004', 1,
                            'New exam question', '{"type":"CHOICE","correct":"A"}');
                    UPDATE section_questions SET question_version_id = '90000000-0000-4000-8000-000000000005'
                    WHERE section_id = '90000000-0000-4000-8000-000000000003';
                    INSERT INTO content_package_versions (id, package_id, version_number)
                    VALUES ('90000000-0000-4000-8000-000000000006', '90000000-0000-4000-8000-000000000001', 2);
                    INSERT INTO content_sections (id, package_version_id, title, sort_order)
                    VALUES ('90000000-0000-4000-8000-000000000007', '90000000-0000-4000-8000-000000000006', 'S2', 1);
                    INSERT INTO question_versions (id, question_id, version_number, stem, answer_spec)
                    VALUES ('90000000-0000-4000-8000-000000000008', '90000000-0000-4000-8000-000000000004', 2,
                            'Revised exam question', '{"type":"CHOICE","correct":"A"}');
                    INSERT INTO section_questions (section_id, question_version_id, sort_order)
                    VALUES ('90000000-0000-4000-8000-000000000007', '90000000-0000-4000-8000-000000000008', 1);
                    """);
            UUID newQuestion = UUID.fromString("90000000-0000-4000-8000-000000000004");
            UUID newVersion = UUID.fromString("90000000-0000-4000-8000-000000000005");
            UUID samePackageVersion = UUID.fromString("90000000-0000-4000-8000-000000000006");
            assertThat(sameTransaction.questionsUsedElsewhere(mockVersion)).isEmpty();
            assertThat(sameTransaction.questionsUsedElsewhere(samePackageVersion)).isEmpty();
            assertThat(sameTransaction.questionsWithWrongPurpose(mockVersion, QuestionPurpose.EXAM)).isEmpty();
            assertThat(sameTransaction.questionsWithWrongPurpose(mockVersion, QuestionPurpose.LEARNING))
                    .containsExactly(newQuestion);
            assertThat(sameTransaction.questionVersionsReservedForLearning(List.of(newVersion)))
                    .containsExactly(newVersion);
            update(connection, """
                    INSERT INTO content_packages (id, code, title, package_type)
                    VALUES ('90000000-0000-4000-8000-000000000009', 'MOCK-OTHER', 'Other', 'MOCK_TEST');
                    UPDATE content_package_versions SET package_id = '90000000-0000-4000-8000-000000000009'
                    WHERE id = '90000000-0000-4000-8000-000000000006';
                    """);
            assertThat(sameTransaction.questionsUsedElsewhere(mockVersion)).isEmpty();
            update(connection, "UPDATE content_package_versions SET status = 'PUBLISHED' WHERE id = '" + samePackageVersion + "'");
            assertThat(sameTransaction.questionsUsedElsewhere(mockVersion))
                    .containsExactly(new QuestionUsageConflict(newQuestion, QuestionUsageConflict.OwnerType.PACKAGE, "MOCK-OTHER"));
            return null;
        });
    }

    @Test
    void seedQuestionsMatchTheirOwnersPurposeAndPublishingLocksHoldUntilRollback() {
        List<UUID> wrongPurpose = jdbc.queryForList("""
                SELECT q.id FROM section_questions sq
                JOIN question_versions qv ON qv.id = sq.question_version_id
                JOIN questions q ON q.id = qv.question_id
                JOIN content_sections s ON s.id = sq.section_id
                JOIN content_package_versions v ON v.id = s.package_version_id
                JOIN content_packages p ON p.id = v.package_id
                WHERE v.status = 'PUBLISHED'
                  AND p.package_type IN ('PRACTICE_SET', 'TOPIC_TEST', 'MOCK_TEST', 'PLACEMENT_TEST')
                  AND q.purpose <> CASE p.package_type WHEN 'MOCK_TEST' THEN 'EXAM'
                                                       WHEN 'PLACEMENT_TEST' THEN 'PLACEMENT'
                                                       ELSE 'LEARNING' END
                """, Map.of(), UUID.class);
        assertThat(wrongPurpose).as("Seed questions with incorrect purpose").isEmpty();
        UUID versionId = reader.publishedTestPackages(DEMO_READING).get(0).packageVersionId();
        rollbackAfter(first -> {
            readerOn(first).lockQuestionsForPublishing(versionId);
            rollbackAfter(second -> {
                update(second, "SET LOCAL lock_timeout = '100ms'");
                DataAccessException lockFailure = assertThrows(DataAccessException.class,
                        () -> readerOn(second).lockQuestionsForPublishing(versionId));
                assertThat(lockFailure.getMostSpecificCause()).isInstanceOf(SQLException.class);
                assertThat(((SQLException) lockFailure.getMostSpecificCause()).getSQLState()).isEqualTo("55P03");
                return null;
            });
            return null;
        });
        rollbackAfter(connection -> {
            readerOn(connection).lockQuestionsForPublishing(versionId);
            return null;
        });
    }

    @Test
    void questionPurposeMigrationBackfillsExistingExamQuestionsAndDefaultsNewQuestions() throws Exception {
        try {
            Flyway.configure().dataSource(dataSource).schemas("purpose_backfill").defaultSchema("purpose_backfill")
                    .locations("classpath:db/migration").target("16").load().migrate();
            try (Connection connection = dataSource.getConnection()) {
                update(connection, """
                        SET search_path TO purpose_backfill;
                        INSERT INTO content_packages (id, code, title, package_type)
                        VALUES ('90000000-0000-4000-8000-000000000001', 'OLD-MOCK', 'Mock', 'MOCK_TEST');
                        INSERT INTO content_package_versions (id, package_id, version_number)
                        VALUES ('90000000-0000-4000-8000-000000000002', '90000000-0000-4000-8000-000000000001', 1);
                        INSERT INTO content_sections (id, package_version_id, title, sort_order)
                        VALUES ('90000000-0000-4000-8000-000000000003', '90000000-0000-4000-8000-000000000002', 'S', 1);
                        INSERT INTO questions (id, question_type)
                        VALUES ('90000000-0000-4000-8000-000000000004', 'MULTIPLE_CHOICE');
                        INSERT INTO question_versions (id, question_id, version_number, stem, answer_spec)
                        VALUES ('90000000-0000-4000-8000-000000000005', '90000000-0000-4000-8000-000000000004', 1,
                                'Legacy exam question', '{"type":"CHOICE","correct":"A"}');
                        INSERT INTO section_questions (section_id, question_version_id, sort_order)
                        VALUES ('90000000-0000-4000-8000-000000000003', '90000000-0000-4000-8000-000000000005', 1);
                        """);
            }
            Flyway.configure().dataSource(dataSource).schemas("purpose_backfill").defaultSchema("purpose_backfill")
                    .locations("classpath:db/migration").load().migrate();
            assertThat(jdbc.queryForObject("SELECT purpose FROM purpose_backfill.questions WHERE id = :id",
                    Map.of("id", UUID.fromString("90000000-0000-4000-8000-000000000004")), String.class)).isEqualTo("EXAM");
            execute("INSERT INTO purpose_backfill.questions (id, question_type) "
                    + "VALUES ('90000000-0000-4000-8000-000000000006', 'MULTIPLE_CHOICE')");
            assertThat(jdbc.queryForObject("SELECT purpose FROM purpose_backfill.questions WHERE id = :id",
                    Map.of("id", UUID.fromString("90000000-0000-4000-8000-000000000006")), String.class)).isEqualTo("LEARNING");
            assertThrows(SQLException.class, () -> execute("UPDATE purpose_backfill.questions SET purpose = 'INVALID'"));
        } finally {
            execute("DROP SCHEMA IF EXISTS purpose_backfill CASCADE");
        }
    }

    @Test
    void practiceSetQuestionsAreNeverTestQuestions() {
        Integer overlap = jdbc.queryForObject("""
                SELECT count(*) FROM section_questions sq
                JOIN content_sections s ON s.id = sq.section_id
                JOIN content_package_versions v ON v.id = s.package_version_id
                JOIN content_packages p ON p.id = v.package_id
                WHERE p.package_type = 'PRACTICE_SET'
                  AND sq.question_version_id IN (
                      SELECT tsq.question_version_id FROM section_questions tsq
                      JOIN content_sections ts ON ts.id = tsq.section_id
                      JOIN content_package_versions tv ON tv.id = ts.package_version_id
                      JOIN content_packages tp ON tp.id = tv.package_id
                      WHERE tp.package_type = 'TOPIC_TEST')
                """, Map.of(), Integer.class);
        assertThat(overlap).isZero();
    }

    // ---- reads ----

    @Test
    void topicSequenceListsEachSkillsTopicsWithLessonsAndTheirActiveKnowledgePoints() {
        List<TopicSequenceResult> sequence = reader.topicSequence(3);

        assertThat(sequence).extracting(TopicSequenceResult::code)
                .containsExactly("DEMO_LISTENING", "LISTENING_6_5_DETAIL", "DEMO_READING", "TFNG_SKILLS",
                        "PREMIUM_MATCHING_INFO", "PREMIUM_SENTENCE_COMPLETION", "READING_6_5_INFERENCE", "DEMO_WRITING",
                        "WRITING_6_5_DISCUSSION", "TREES_MULTI_SKILL");
        assertThat(sequence).extracting(TopicSequenceResult::skill)
                .containsExactly(Skill.LISTENING, Skill.LISTENING, Skill.READING, Skill.READING, Skill.READING, Skill.READING,
                        Skill.READING, Skill.WRITING, Skill.WRITING, null);
        // Writing topics have no final test; a learner passes them by completing their lessons. The multi-skill topic
        // has no single skill and comes last.
        assertThat(sequence).extracting(TopicSequenceResult::hasTopicTest)
                .containsExactly(true, true, true, true, true, true, true, false, false, true);
        // The lower-band Reading course keeps its paid topics after its free topics.
        assertThat(sequence).extracting(TopicSequenceResult::requiredFeatureKey)
                .containsExactly(null, null, null, null, "PREMIUM_CONTENT", "PREMIUM_CONTENT", null, null, null, null);
        assertThat(sequence).allMatch(entry -> entry.course().hasCourseTest());
        assertThat(topic(sequence, "READING_6_5_INFERENCE").course().bandLevel()).isEqualByComparingTo("6.5");
        assertThat(topic(sequence, "DEMO_READING").course().bandLevel()).isEqualByComparingTo("5.5");
        TopicSequenceResult reading = topic(sequence, "DEMO_READING");
        assertThat(reading.lessonCount()).isEqualTo(4);
        assertThat(reading.knowledgePoints()).extracting(TopicSequenceResult.KnowledgePointEntry::code)
                .containsExactly("DEMO_READING_MAIN_IDEA", "DR_IDEA_OR_DETAIL", "DR_TOPIC_SENTENCE",
                        "DR_MATCHING_HEADINGS");
        assertThat(reading.knowledgePoints()).allMatch(TopicSequenceResult.KnowledgePointEntry::hasPracticeSet);
        // The Writing knowledge points moved with their essays and have no practice set.
        assertThat(topic(sequence, "DEMO_WRITING").knowledgePoints())
                .extracting(TopicSequenceResult.KnowledgePointEntry::code, TopicSequenceResult.KnowledgePointEntry::hasPracticeSet)
                .containsExactly(tuple("DEMO_READING_W2_OPINION", false), tuple("DEMO_READING_W1_CHART", false));
        assertThat(topic(sequence, "TFNG_SKILLS").knowledgePoints()).singleElement()
                .satisfies(kp -> {
                    assertThat(kp.code()).isEqualTo("TFNG_FALSE_VS_NOT_GIVEN");
                    // V16 gave TF1 its first practice set.
                    assertThat(kp.hasPracticeSet()).isTrue();
                });
    }

    @Test
    void writingLessonsLiveInTheirOwnTopicWithTheirKnowledgePoints() {
        assertThat(reader.publishedLesson(lessonId("W1")).orElseThrow().knowledgePointIds())
                .containsExactly(kpId("DEMO_READING_W1_CHART"));
        assertThat(reader.publishedLesson(lessonId("W2")).orElseThrow().knowledgePointIds())
                .containsExactly(kpId("DEMO_READING_W2_OPINION"));

        // The Reading lessons L3 and L4 keep only Reading knowledge points.
        for (String code : List.of("L3", "L4")) {
            assertThat(reader.publishedLesson(lessonId(code)).orElseThrow().knowledgePointIds()).as(code)
                    .doesNotContain(kpId("DEMO_READING_W1_CHART"), kpId("DEMO_READING_W2_OPINION"));
        }
        // A topic with published lessons and no skill label must be one whose lessons teach several skills.
        assertThat(reader.topicSequence(3)).filteredOn(topic -> topic.skill() == null)
                .allSatisfy(topic -> assertThat(topic.skills()).as(topic.code()).hasSizeGreaterThan(1));
        assertThat(jdbc.queryForList("SELECT code FROM topics WHERE skill IS NULL AND id IN "
                + "(SELECT topic_id FROM lessons WHERE status = 'PUBLISHED')", Map.of(), String.class))
                .containsExactly("TREES_MULTI_SKILL");
    }

    @Test
    void everyLessonPracticeSetStaysWithinTheSkillsItsLessonTeaches() {
        // A lesson may teach several skills; its Practice may only measure skills the lesson teaches.
        List<Map<String, Object>> practiceSets = jdbc.queryForList("""
                SELECT p.code, p.current_published_version_id AS version_id, p.lesson_id
                FROM content_packages p
                WHERE p.package_type = 'PRACTICE_SET' AND p.status = 'PUBLISHED' AND p.lesson_id IS NOT NULL
                """, Map.of());

        assertThat(practiceSets).isNotEmpty();
        assertThat(practiceSets).filteredOn(row -> reader.packageVersionLeavesLessonSkills(
                        (UUID) row.get("version_id"), (UUID) row.get("lesson_id")))
                .extracting(row -> row.get("code")).isEmpty();
    }

    @Test
    void topicSkillAcceptsOnlyOneOfTheFourSkills() {
        assertThrows(SQLException.class, () -> execute(
                "UPDATE topics SET skill = 'ALL' WHERE id = '" + DEMO_READING + "'"));
    }

    @Test
    void aPracticeSetBelowTheMinimumIsNeverFound() {
        // KP5 gets one extra one-question practice set; it stays below the default minimum of three questions.
        List<String> found = rollbackAfter(connection -> {
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
            return sameTransaction.searchPracticeSets(kpId("TFNG_FALSE_VS_NOT_GIVEN"), List.of(), 3, 10, null).stream()
                    .map(PracticeSetResult::code).toList();
        });
        assertThat(found).containsExactly("PS-TF-A", "PS-TF-B", "PS-TF-C", "PS-TF-D");
    }

    @Test
    void practiceSetSearchHonoursExclusionsAndSkipsTheOneQuestionDemoPackage() {
        UUID kp2 = kpId("DR_IDEA_OR_DETAIL");
        PracticeSetResult first = reader.searchPracticeSets(kp2, List.of(), 3, 1, null).get(0);
        PracticeSetResult second = reader.searchPracticeSets(kp2, List.of(first.packageId()), 3, 1, null).get(0);
        assertThat(Set.of(first.code(), second.code())).containsExactlyInAnyOrder("PS-KP2-A", "PS-KP2-B");
        // The four-question sets come before the three-question V18 set.
        PracticeSetResult third = reader.searchPracticeSets(kp2, List.of(first.packageId(), second.packageId()), 3, 1,
                null).get(0);
        assertThat(third.code()).isEqualTo("PS-KP2-C");
        // PS-KP2-D is review-only material and matches all three questions, so it comes next.
        PracticeSetResult reviewOnly = reader.searchPracticeSets(kp2,
                List.of(first.packageId(), second.packageId(), third.packageId()), 3, 1, null).get(0);
        assertThat(reviewOnly.code()).isEqualTo("PS-KP2-D");
        // PS-KP3-B mixes three knowledge points; its one idea-or-detail question makes it the last set left for KP2.
        PracticeSetResult mixed = reader.searchPracticeSets(kp2,
                List.of(first.packageId(), second.packageId(), third.packageId(), reviewOnly.packageId()), 3, 1, null)
                .get(0);
        assertThat(mixed.code()).isEqualTo("PS-KP3-B");
        assertThat(mixed.questionCount()).isEqualTo(3);
        assertThat(mixed.matchedQuestionCount()).isEqualTo(1);
        assertThat(reader.searchPracticeSets(kp2, List.of(first.packageId(), second.packageId(), third.packageId(),
                reviewOnly.packageId(), mixed.packageId()), 3, 1, null)).isEmpty();
        assertThat(first.questionCount()).isEqualTo(4);
        assertThat(first.matchedQuestionCount()).isEqualTo(4);

        List<PracticeSetResult> kp1 = reader.searchPracticeSets(kpId("DEMO_READING_MAIN_IDEA"), List.of(), 3, 10, null);
        // Four questions each for A and B, three for the V16 sets C and D.
        assertThat(kp1).extracting(PracticeSetResult::code).containsExactly("PS-KP1-A", "PS-KP1-B", "PS-KP1-C", "PS-KP1-D");
    }

    @Test
    void aPreferredLessonsPracticeSetsComeFirstAndTheRestKeepTheirOrder() {
        List<String> codes = rollbackAfter(connection -> {
            update(connection, "UPDATE content_packages SET lesson_id = '" + lessonId("L4") + "' WHERE code = 'PS-KP1-D'");
            // A set without a lesson keeps its place by matching questions, ahead of smaller sets of other lessons.
            update(connection, "UPDATE content_packages SET lesson_id = NULL WHERE code = 'PS-KP1-A'");
            return readerOn(connection).searchPracticeSets(kpId("DEMO_READING_MAIN_IDEA"), List.of(), 3, 10,
                    lessonId("L4")).stream().map(PracticeSetResult::code).toList();
        });
        assertThat(codes).containsExactly("PS-KP1-D", "PS-KP1-A", "PS-KP1-B", "PS-KP1-C");
    }

    @Test
    void practiceSetsBelongToTheEarliestLessonTeachingTheirKnowledgePoint() {
        Map<String, String> lessonOfPackage = new java.util.TreeMap<>();
        jdbc.query("""
                SELECT p.code, l.code AS lesson FROM content_packages p JOIN lessons l ON l.id = p.lesson_id
                WHERE p.package_type = 'PRACTICE_SET'
                """, Map.of(), rs -> {
            lessonOfPackage.put(rs.getString("code"), rs.getString("lesson"));
        });
        assertThat(lessonOfPackage).containsExactlyEntriesOf(new java.util.TreeMap<>(Map.ofEntries(
                Map.entry("PS-KP1-A", "L2"), Map.entry("PS-KP1-B", "L2"), Map.entry("PS-KP2-A", "L3"), Map.entry("PS-KP2-B", "L3"),
                Map.entry("PS-KP3-A", "L1"), Map.entry("PS-KP4-A", "L4"), Map.entry("PS-NUM", "LS1"),
                Map.entry("PS-SPELL", "LS1"), Map.entry("PS-PARA", "LS2"), Map.entry("PS-TRAP", "LS2"),
                Map.entry("PS-TF-A", "TF1"), Map.entry("PS-PM1-A", "PM1"), Map.entry("PS-PM2-A", "PM2"),
                Map.entry("PS-PS1-A", "PS1"), Map.entry("PS-PS2-A", "PS2"),
                Map.entry("PS-KP3-B", "L1"), Map.entry("PS-KP4-B", "L4"), Map.entry("PS-TF-B", "TF1"),
                Map.entry("PS-PM1-B", "PM1"), Map.entry("PS-PM2-B", "PM2"),
                Map.entry("PS-PS1-B", "PS1"), Map.entry("PS-PS2-B", "PS2"),
                Map.entry("R65-PS-INFERENCE", "R65-I1"), Map.entry("R65-PS-PARAPHRASE", "R65-I1"),
                Map.entry("L65-PS-CORRECTION", "L65-1"), Map.entry("L65-PS-PURPOSE", "L65-1"),
                Map.entry("TR-PS-READING", "T1"), Map.entry("TR-PS-LISTENING", "T1"), Map.entry("TR-PS-WRITING", "T1"),
                Map.entry("TR-PS-MIXED", "T1"), Map.entry("TR-PS-T2-READING", "T2"))));
        // A lesson requires every set bound to it, so the sets reviews draw from are bound to no lesson.
        assertThat(jdbc.queryForList("SELECT code FROM content_packages WHERE package_type = 'PRACTICE_SET' "
                + "AND lesson_id IS NULL ORDER BY code", Map.of(), String.class)).containsExactly("DEMO_MAIN_FLOW_READING",
                "PS-KP1-C", "PS-KP1-D", "PS-KP2-C", "PS-KP2-D", "PS-KP3-C", "PS-KP3-D", "PS-KP4-C", "PS-KP4-D", "PS-TF-C",
                "PS-TF-D");
        // The one-question V4 demo package is too small to be anyone's Practice.
        assertThat(jdbc.queryForObject("SELECT lesson_id FROM content_packages WHERE code = 'DEMO_MAIN_FLOW_READING'",
                Map.of(), UUID.class)).isNull();
        assertThrows(SQLException.class, () -> execute("UPDATE content_packages SET lesson_id = '" + lessonId("L1")
                + "' WHERE package_type = 'TOPIC_TEST'"));
    }

    @Test
    void everyLessonOfATopicWithAFinalTestOffersPractice() {
        List<String> withoutPractice = jdbc.queryForList("""
                SELECT l.code FROM lessons l
                WHERE l.status = 'PUBLISHED'
                  AND EXISTS (SELECT 1 FROM content_packages tp WHERE tp.topic_id = l.topic_id AND tp.package_type = 'TOPIC_TEST')
                  AND NOT EXISTS (SELECT 1 FROM content_packages p WHERE p.lesson_id = l.id AND p.status = 'PUBLISHED')
                """, Map.of(), String.class);
        assertThat(withoutPractice).isEmpty();
        // Writing lessons belong to a topic without a final test and need no Practice.
        assertThat(reader.lessonPracticeSets(lessonId("W1"))).isEmpty();
    }

    @Test
    void newPracticeQuestionsAreEligibleAndCarryExplanationsAndHints() {
        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT qv.explanation, qv.hint, q.skill FROM question_versions qv JOIN questions q ON q.id = qv.question_id
                WHERE qv.id::text LIKE '26000000-%'
                """, Map.of());
        assertThat(rows).hasSize(21).allSatisfy(row -> {
            assertThat((String) row.get("explanation")).isNotBlank();
            assertThat((String) row.get("hint")).isNotBlank();
            assertThat(row.get("skill")).isEqualTo("READING");
        });
        for (String code : List.of("PS-TF-A", "PS-PM1-A", "PS-PM2-A", "PS-PS1-A", "PS-PS2-A")) {
            UUID lesson = jdbc.queryForObject("SELECT lesson_id FROM content_packages WHERE code = :code",
                    Map.of("code", code), UUID.class);
            assertThat(reader.lessonPracticeSets(lesson)).as(code)
                    .anySatisfy(set -> {
                        assertThat(set.code()).isEqualTo(code);
                        assertThat(set.questionCount()).isEqualTo(3);
                        assertThat(set.knowledgePointIds()).hasSize(1);
                    });
        }
        assertThat(reader.lessonPracticeSets(lessonId("PM1")))
                .allSatisfy(set -> assertThat(set.requiredFeatureKey()).isEqualTo("PREMIUM_CONTENT"));
    }

    @Test
    void everyReadingKnowledgePointHasTwoReviewOnlySets() {
        Map<String, Long> bySet = new java.util.TreeMap<>();
        jdbc.query("""
                SELECT kp.code, count(DISTINCT p.id) AS sets
                FROM content_packages p
                JOIN content_sections s ON s.package_version_id = p.current_published_version_id
                JOIN section_questions sq ON sq.section_id = s.id
                JOIN question_knowledge_points qkp ON qkp.question_version_id = sq.question_version_id
                JOIN knowledge_points kp ON kp.id = qkp.knowledge_point_id
                WHERE p.package_type = 'PRACTICE_SET' AND p.lesson_id IS NULL AND p.status = 'PUBLISHED'
                GROUP BY kp.code
                """, Map.of(), rs -> {
            bySet.put(rs.getString("code"), rs.getLong("sets"));
        });
        for (String code : List.of("DEMO_READING_MAIN_IDEA", "DR_IDEA_OR_DETAIL", "DR_TOPIC_SENTENCE",
                "DR_MATCHING_HEADINGS", "TFNG_FALSE_VS_NOT_GIVEN")) {
            assertThat(bySet.get(code)).as(code).isGreaterThanOrEqualTo(2L);
        }
    }

    @Test
    void urbanCyclingPracticeSetMixesThreeKnowledgePoints() {
        assertThat(reader.lessonPracticeSets(lessonId("L1")))
                .filteredOn(set -> set.code().equals("PS-KP3-B")).singleElement()
                .satisfies(set -> {
                    assertThat(set.questionCount()).isEqualTo(3);
                    assertThat(set.knowledgePointIds()).containsExactlyInAnyOrder(kpId("DR_TOPIC_SENTENCE"),
                            kpId("DR_IDEA_OR_DETAIL"), kpId("DR_MATCHING_HEADINGS"));
                });
    }

    @Test
    void practiceSetItemsCarryTheirHints() {
        UUID version = jdbc.queryForObject("SELECT current_published_version_id FROM content_packages WHERE code = 'PS-TF-A'",
                Map.of(), UUID.class);
        assertThat(reader.publishedPackageVersion(version).orElseThrow().sections()).singleElement()
                .satisfies(section -> assertThat(section.items()).hasSize(3)
                        .allSatisfy(item -> assertThat(item.hint()).isNotBlank()));
    }

    @Test
    void topicPracticeSetsListEveryLessonInOrderInOneRead() {
        UUID listening = jdbc.queryForObject("SELECT id FROM topics WHERE code = 'DEMO_LISTENING'", Map.of(), UUID.class);
        List<LessonPracticeSetsResult> lessons = reader.topicPracticeSets(listening);

        assertThat(lessons).extracting(LessonPracticeSetsResult::lessonId).containsExactly(lessonId("LS1"), lessonId("LS2"));
        assertThat(lessons.get(0).practiceSets()).extracting(LessonPracticeSetResult::code)
                .containsExactly("PS-NUM", "PS-SPELL");
        assertThat(reader.topicPracticeSets(UUID.randomUUID())).isEmpty();
    }

    @Test
    void availabilityCountsEligibleSetsPerKnowledgePointLeavingOutExcludedOnes() {
        UUID kp1 = kpId("DEMO_READING_MAIN_IDEA");
        UUID kp5 = kpId("TFNG_FALSE_VS_NOT_GIVEN");
        UUID unknown = UUID.randomUUID();
        UUID setA = jdbc.queryForObject("SELECT id FROM content_packages WHERE code = 'PS-KP1-A'", Map.of(), UUID.class);

        assertThat(reader.countEligiblePracticeSets(List.of(kp1, kp5, unknown), List.of(setA), 3))
                .containsExactlyInAnyOrderEntriesOf(Map.of(kp1, 3, kp5, 4, unknown, 0));
    }

    @Test
    void lessonsTeachTheSkillsOfTheirKnowledgePointsAndHaveNoBlocks() {
        LessonContentResult l1 = reader.publishedLesson(lessonId("L1")).orElseThrow();
        assertThat(l1.skill()).isEqualTo(Skill.READING);
        assertThat(l1.knowledgePointIds()).containsExactly(kpId("DR_TOPIC_SENTENCE"));

        for (String table : List.of("lesson_blocks", "lesson_block_questions", "lesson_block_vocabulary",
                "lesson_block_knowledge_points")) {
            assertThat(jdbc.queryForObject("SELECT to_regclass('" + table + "')::text", Map.of(), String.class))
                    .as(table).isNull();
        }
        // Every lesson still has at least one knowledge point, so it still has a skill.
        assertThat(jdbc.queryForList("""
                SELECT l.code FROM lessons l
                WHERE NOT EXISTS (SELECT 1 FROM lesson_knowledge_points lkp WHERE lkp.lesson_id = l.id)
                """, Map.of(), String.class)).isEmpty();
    }

    @Test
    void aPackageVersionLeavesTheLessonSkillWhenAQuestionHasAnotherSkill() {
        UUID numVersion = jdbc.queryForObject("SELECT current_published_version_id FROM content_packages WHERE code = 'PS-NUM'",
                Map.of(), UUID.class);
        UUID topicSentenceVersion = jdbc.queryForObject(
                "SELECT current_published_version_id FROM content_packages WHERE code = 'PS-KP3-A'", Map.of(), UUID.class);
        assertThat(reader.packageVersionLeavesLessonSkills(numVersion, lessonId("L1"))).isTrue();
        assertThat(reader.packageVersionLeavesLessonSkills(topicSentenceVersion, lessonId("L1"))).isFalse();
    }

    @Test
    void topicLessonsAreInOrderWithTheirKnowledgePointsAndSkills() {
        List<LessonSummaryResult> lessons = reader.publishedLessons(DEMO_READING);

        assertThat(lessons).extracting(LessonSummaryResult::code).containsExactly("L1", "L2", "L3", "L4");
        assertThat(lessons.get(0).skills()).containsExactly(Skill.READING);
        assertThat(lessons).extracting(LessonSummaryResult::estimatedMinutes).allMatch(minutes -> minutes > 0);
        assertThat(lessons.get(3).knowledgePointIds())
                .containsExactly(kpId("DEMO_READING_MAIN_IDEA"), kpId("DR_MATCHING_HEADINGS"));
    }

    @Test
    void lessonContentCarriesItsSummaryMarkdownAndKnowledgePoints() {
        LessonContentResult l1 = reader.publishedLesson(lessonId("L1")).orElseThrow();
        assertThat(l1.summaryMarkdown()).startsWith("## Learning Outcomes");
        assertThat(l1.knowledgePointIds()).containsExactly(kpId("DR_TOPIC_SENTENCE"));
        // L2 had no summary: the theory text of its old TEXT block moved there.
        assertThat(reader.publishedLesson(lessonId("L2")).orElseThrow().summaryMarkdown())
                .startsWith("Ý chính của cả bài là điều mọi đoạn cùng góp vào.");
        assertThat(reader.publishedLesson(UUID.randomUUID())).isEmpty();
    }

    @Test
    void listeningTopicHasAudioLessonsOnePracticeSetPerKnowledgePointAndTwoAudioTests() throws Exception {
        TopicSequenceResult listening = topic(reader.topicSequence(3), "DEMO_LISTENING");
        assertThat(listening.knowledgePoints()).extracting(TopicSequenceResult.KnowledgePointEntry::code)
                .containsExactly("LS_NUM", "LS_SPELL", "LS_PARA", "LS_TRAP");
        assertThat(listening.knowledgePoints()).allMatch(TopicSequenceResult.KnowledgePointEntry::hasPracticeSet);

        // Each listening KP has exactly one three-question practice set, every set and test has one audio section.
        List<Map<String, Object>> packages = jdbc.queryForList("""
                SELECT p.code, p.package_type, count(DISTINCT sq.question_version_id) AS questions,
                       count(DISTINCT qkp.knowledge_point_id) AS kps, min(a.media_reference) AS audio_key
                FROM content_packages p
                JOIN content_sections s ON s.package_version_id = p.current_published_version_id
                JOIN section_questions sq ON sq.section_id = s.id
                JOIN question_knowledge_points qkp ON qkp.question_version_id = sq.question_version_id
                JOIN content_asset_links al ON al.section_id = s.id
                JOIN content_assets a ON a.id = al.asset_id AND a.asset_type = 'AUDIO'
                JOIN knowledge_points kp ON kp.id = qkp.knowledge_point_id
                JOIN topics t ON t.id = kp.topic_id AND t.code = 'DEMO_LISTENING'
                WHERE s.skill = 'LISTENING'
                GROUP BY p.code, p.package_type ORDER BY p.code
                """, Map.of());
        assertThat(packages).extracting(r -> r.get("code"))
                .containsExactly("PS-NUM", "PS-PARA", "PS-SPELL", "PS-TRAP", "X3", "X4");
        assertThat(packages).allSatisfy(r -> {
            boolean test = "TOPIC_TEST".equals(r.get("package_type"));
            assertThat(((Number) r.get("questions")).intValue()).isEqualTo(test ? 4 : 3);
            assertThat(((Number) r.get("kps")).intValue()).isEqualTo(test ? 4 : 1);
            assertThat((String) r.get("audio_key")).matches("listening/demo/\\w+\\.mp3");
        });

        UUID x3Version = reader.publishedTestPackages(listening.topicId()).stream()
                .filter(p -> p.code().equals("X3")).findFirst().orElseThrow().packageVersionId();
        PackageVersionContentResult x3 = new GetPackageVersionContentUseCase(reader, MEDIA).execute(x3Version);
        assertThat(x3.sections()).singleElement().satisfies(section -> {
            assertThat(section.passage()).isNull();
            assertThat(section.audio().mediaUrl())
                    .isEqualTo("https://media.example.test/ieltspath/listening/demo/hotel.mp3");
            assertThat(section.audio().transcript()).contains("Delaney. D, E, L, A, N, E, Y.");
            assertThat(section.items()).hasSize(4);
        });
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
    void practiceAndTestQuestionsAreReservedForLearning() {
        List<UUID> all = jdbc.queryForList("SELECT id FROM question_versions", Map.of(), UUID.class);
        Set<UUID> reserved = reader.questionVersionsReservedForLearning(all);
        // Practice, final-test, course-test, mock-test and placement-test packages reserve their questions; the
        // questions that lessons used to hold are not reserved by anything now.
        Set<UUID> inPackages = new java.util.HashSet<>(jdbc.queryForList("""
                SELECT DISTINCT sq.question_version_id FROM section_questions sq
                JOIN content_sections s ON s.id = sq.section_id
                JOIN content_package_versions v ON v.id = s.package_version_id
                JOIN content_packages p ON p.id = v.package_id
                WHERE p.package_type IN ('PRACTICE_SET', 'TOPIC_TEST', 'COURSE_TEST', 'MOCK_TEST', 'PLACEMENT_TEST')
                """, Map.of(), UUID.class));
        assertThat(reserved).isNotEmpty().isEqualTo(inPackages);
        assertThat(reader.questionVersionsReservedForLearning(List.of(UUID.randomUUID()))).isEmpty();
    }

    // ---- helpers ----

    private static TopicSequenceResult topic(List<TopicSequenceResult> sequence, String code) {
        return sequence.stream().filter(topic -> topic.code().equals(code)).findFirst().orElseThrow();
    }

    private static UUID lessonId(String code) {
        return jdbc.queryForObject("SELECT id FROM lessons WHERE code = :code", Map.of("code", code), UUID.class);
    }

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
