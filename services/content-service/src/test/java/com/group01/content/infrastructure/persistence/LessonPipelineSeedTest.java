package com.group01.content.infrastructure.persistence;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.group01.content.application.result.LessonContentResult;
import com.group01.content.application.result.LessonPracticeSetResult;
import com.group01.content.application.result.LessonPracticeSetsResult;
import com.group01.content.application.result.LessonSummaryResult;
import com.group01.content.application.result.PackageVersionContentResult;
import com.group01.content.application.result.PracticeSetResult;
import com.group01.content.application.result.TopicSequenceResult;
import com.group01.content.application.usecase.GetLessonContentUseCase;
import com.group01.content.application.usecase.GetPackageVersionContentUseCase;
import com.group01.content.domain.vo.AssetType;
import com.group01.content.domain.vo.BlockType;
import com.group01.content.domain.vo.LessonBlockKind;
import com.group01.content.domain.vo.MediaReferencePolicy;
import com.group01.content.domain.vo.PackageType;
import com.group01.content.domain.vo.Skill;
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
import java.nio.charset.StandardCharsets;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Base64;
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
            assertThat(containerOf(versionId)).as(code).contains(item.get("container").asText());
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
    void topicSequenceListsEachSkillsTopicsWithLessonsAndTheirActiveKnowledgePoints() {
        List<TopicSequenceResult> sequence = reader.topicSequence(3);

        assertThat(sequence).extracting(TopicSequenceResult::code)
                .containsExactly("DEMO_LISTENING", "DEMO_READING", "TFNG_SKILLS",
                        "PREMIUM_MATCHING_INFO", "PREMIUM_SENTENCE_COMPLETION", "DEMO_WRITING");
        assertThat(sequence).extracting(TopicSequenceResult::skill)
                .containsExactly(Skill.LISTENING, Skill.READING, Skill.READING, Skill.READING, Skill.READING,
                        Skill.WRITING);
        // The Writing topic has no final test; a learner passes it by completing its lessons.
        assertThat(sequence).extracting(TopicSequenceResult::hasTopicTest)
                .containsExactly(true, true, true, true, true, false);
        // Within the Reading track, paid topics come after every free one.
        assertThat(sequence).extracting(TopicSequenceResult::requiredFeatureKey)
                .containsExactly(null, null, null, "PREMIUM_CONTENT", "PREMIUM_CONTENT", null);
        TopicSequenceResult reading = topic(sequence, "DEMO_READING");
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
    void writingEssaysMovedToTheirOwnTopicKeepingTheirBlockIds() {
        UUID w1 = lessonId("W1");
        UUID w2 = lessonId("W2");
        assertThat(blockIds(w1)).containsExactly(UUID.fromString("22000000-0000-4000-8000-040000000001"),
                UUID.fromString("22000000-0000-4000-8000-040000000002"));
        assertThat(blockIds(w2)).containsExactly(UUID.fromString("21000000-0000-4000-8000-040000000001"),
                UUID.fromString("21000000-0000-4000-8000-040000000002"));
        assertThat(reader.publishedLesson(w1).orElseThrow().knowledgePointIds())
                .containsExactly(kpId("DEMO_READING_W1_CHART"));
        assertThat(reader.publishedLesson(w2).orElseThrow().knowledgePointIds())
                .containsExactly(kpId("DEMO_READING_W2_OPINION"));

        // The Reading lessons L3 and L4 keep only Reading blocks and knowledge points.
        for (String code : List.of("L3", "L4")) {
            UUID lessonId = lessonId(code);
            assertThat(blockIds(lessonId)).as(code).hasSize(3);
            assertThat(reader.publishedLesson(lessonId).orElseThrow().knowledgePointIds()).as(code)
                    .doesNotContain(kpId("DEMO_READING_W1_CHART"), kpId("DEMO_READING_W2_OPINION"));
        }
        assertThat(jdbc.queryForList("SELECT code FROM topics WHERE skill IS NULL AND id IN "
                + "(SELECT topic_id FROM lessons WHERE status = 'PUBLISHED')", Map.of(), String.class)).isEmpty();
    }

    @Test
    void everyLessonTeachesOnlyItsTopicsSkill() {
        List<String> mismatchedQuestions = jdbc.queryForList("""
                SELECT l.code || ' / ' || q.skill
                FROM lessons l
                JOIN topics t ON t.id = l.topic_id
                JOIN lesson_blocks b ON b.lesson_id = l.id
                JOIN lesson_block_questions bq ON bq.block_id = b.id
                JOIN question_versions qv ON qv.id = bq.question_version_id
                JOIN questions q ON q.id = qv.question_id
                WHERE l.status = 'PUBLISHED' AND q.skill IS DISTINCT FROM t.skill
                """, Map.of(), String.class);
        List<String> mismatchedKnowledgePoints = jdbc.queryForList("""
                SELECT l.code || ' / ' || kp.code
                FROM lessons l
                JOIN topics t ON t.id = l.topic_id
                JOIN lesson_knowledge_points lkp ON lkp.lesson_id = l.id
                JOIN knowledge_points kp ON kp.id = lkp.knowledge_point_id
                WHERE l.status = 'PUBLISHED' AND kp.skill IS NOT NULL AND kp.skill <> t.skill
                """, Map.of(), String.class);

        assertThat(mismatchedQuestions).isEmpty();
        assertThat(mismatchedKnowledgePoints).isEmpty();
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
        assertThat(found).containsExactly("PS-TF-A");
    }

    @Test
    void practiceSetSearchHonoursExclusionsAndSkipsTheOneQuestionDemoPackage() {
        UUID kp2 = kpId("DR_IDEA_OR_DETAIL");
        PracticeSetResult first = reader.searchPracticeSets(kp2, List.of(), 3, 1, null).get(0);
        PracticeSetResult second = reader.searchPracticeSets(kp2, List.of(first.packageId()), 3, 1, null).get(0);
        assertThat(Set.of(first.code(), second.code())).containsExactlyInAnyOrder("PS-KP2-A", "PS-KP2-B");
        assertThat(reader.searchPracticeSets(kp2, List.of(first.packageId(), second.packageId()), 3, 1, null)).isEmpty();
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
                Map.entry("PS-KP1-A", "L2"), Map.entry("PS-KP1-B", "L2"), Map.entry("PS-KP1-C", "L2"),
                Map.entry("PS-KP1-D", "L2"), Map.entry("PS-KP2-A", "L3"), Map.entry("PS-KP2-B", "L3"),
                Map.entry("PS-KP3-A", "L1"), Map.entry("PS-KP4-A", "L4"), Map.entry("PS-NUM", "LS1"),
                Map.entry("PS-SPELL", "LS1"), Map.entry("PS-PARA", "LS2"), Map.entry("PS-TRAP", "LS2"),
                Map.entry("PS-TF-A", "TF1"), Map.entry("PS-PM1-A", "PM1"), Map.entry("PS-PM2-A", "PM2"),
                Map.entry("PS-PS1-A", "PS1"), Map.entry("PS-PS2-A", "PS2"))));
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
        for (String code : List.of("PS-TF-A", "PS-PM1-A", "PS-PM2-A", "PS-PS1-A", "PS-PS2-A", "PS-KP1-C", "PS-KP1-D")) {
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
                .containsExactlyInAnyOrderEntriesOf(Map.of(kp1, 3, kp5, 1, unknown, 0));
    }

    @Test
    void textBlocksTeachTheirLessonsKnowledgePointsAndExerciseBlocksTheirQuestions() {
        LessonContentResult l1 = reader.publishedLesson(lessonId("L1")).orElseThrow();
        assertThat(l1.skill()).isEqualTo(Skill.READING);
        assertThat(l1.blocks()).extracting(LessonContentResult.Block::blockType, LessonContentResult.Block::knowledgePointIds)
                .containsExactly(tuple(BlockType.TEXT, List.of(kpId("DR_TOPIC_SENTENCE"))),
                        tuple(BlockType.ASSET, List.of()),
                        tuple(BlockType.EXERCISE, List.of(kpId("DR_TOPIC_SENTENCE"))),
                        // "Luyện thêm với đoạn C và D." only leads into the next exercise and teaches nothing.
                        tuple(BlockType.TEXT, List.of()),
                        tuple(BlockType.EXERCISE, List.of(kpId("DR_TOPIC_SENTENCE"))));

        List<String> outsideTheRule = jdbc.queryForList("""
                SELECT b.id::text FROM lesson_block_knowledge_points bk
                JOIN lesson_blocks b ON b.id = bk.block_id
                WHERE b.block_type <> 'TEXT'
                   OR NOT EXISTS (SELECT 1 FROM lesson_knowledge_points lkp
                                  WHERE lkp.lesson_id = b.lesson_id AND lkp.knowledge_point_id = bk.knowledge_point_id)
                """, Map.of(), String.class);
        assertThat(outsideTheRule).isEmpty();
    }

    @Test
    void aPackageVersionLeavesTheLessonSkillWhenAQuestionHasAnotherSkill() {
        UUID numVersion = jdbc.queryForObject("SELECT current_published_version_id FROM content_packages WHERE code = 'PS-NUM'",
                Map.of(), UUID.class);
        UUID topicSentenceVersion = jdbc.queryForObject(
                "SELECT current_published_version_id FROM content_packages WHERE code = 'PS-KP3-A'", Map.of(), UUID.class);
        assertThat(reader.packageVersionLeavesLessonSkill(numVersion, lessonId("L1"))).isTrue();
        assertThat(reader.packageVersionLeavesLessonSkill(topicSentenceVersion, lessonId("L1"))).isFalse();
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
    void everySeededExerciseBlockFollowsTheBlockRuleAndTheWritingLessonsEndWithEssays() {
        GetLessonContentUseCase lessons = new GetLessonContentUseCase(reader, MEDIA);
        List<UUID> lessonIds = jdbc.queryForList("SELECT id FROM lessons", Map.of(), UUID.class);
        List<LessonBlockKind> kinds = new ArrayList<>();
        for (UUID lessonId : lessonIds) {
            lessons.execute(lessonId).blocks().stream()
                    .filter(block -> block.blockType() == BlockType.EXERCISE)
                    .forEach(block -> kinds.add(block.blockKind()));
        }
        assertThat(kinds).doesNotContainNull().filteredOn(LessonBlockKind.ESSAY::equals).hasSize(2);

        LessonContentResult.Block essay = lessons.execute(lessonId("W2")).blocks().get(1);
        assertThat(essay.blockKind()).isEqualTo(LessonBlockKind.ESSAY);
        assertThat(essay.questions()).singleElement().satisfies(q -> {
            assertThat(q.answerSpecJson()).contains("\"task\": \"TASK_2\"");
            assertThat(q.explanation()).startsWith("Many cities now face hotter summers");
            assertThat(q.knowledgePointIds()).containsExactly(kpId("DEMO_READING_W2_OPINION"));
        });
    }

    @Test
    void task1EssayCarriesItsChartImageWhoseFiguresMatchTheChartFacts() throws Exception {
        LessonContentResult.Block essay = new GetLessonContentUseCase(reader, MEDIA).execute(lessonId("W1")).blocks()
                .get(1);

        assertThat(essay.blockKind()).isEqualTo(LessonBlockKind.ESSAY);
        LessonContentResult.Question question = essay.questions().get(0);
        assertThat(question.knowledgePointIds()).containsExactly(kpId("DEMO_READING_W1_CHART"));
        assertThat(question.explanation()).startsWith("The chart compares how hot three kinds of roof");
        JsonNode spec = JSON.readTree(question.answerSpecJson());
        assertThat(spec.get("task").asText()).isEqualTo("TASK_1");
        assertThat(spec.get("minWords").asInt()).isEqualTo(150);

        LessonContentResult.QuestionAsset chart = question.assets().get(0);
        assertThat(question.assets()).hasSize(1);
        assertThat(chart.assetType()).isEqualTo(AssetType.IMAGE);
        assertThat(chart.altText()).startsWith("Bar chart of July afternoon roof temperatures");
        String prefix = "data:image/svg+xml;base64,";
        assertThat(chart.mediaUrl()).startsWith(prefix);
        byte[] svg = Base64.getDecoder().decode(chart.mediaUrl().substring(prefix.length()));
        assertThat(svg.length).isLessThanOrEqualTo(8 * 1024);
        String drawn = new String(svg, StandardCharsets.UTF_8);
        for (String figure : List.of("82", "45", "33", "74", "41", "30", "61", "35", "25")) {
            assertThat(spec.get("chartFacts").asText()).contains(figure);
            assertThat(drawn).as("chart shows " + figure).contains(">" + figure + "<");
        }
    }
    @Test
    void listeningTopicHasAudioLessonsOnePracticeSetPerKnowledgePointAndTwoAudioTests() throws Exception {
        TopicSequenceResult listening = topic(reader.topicSequence(3), "DEMO_LISTENING");
        assertThat(listening.knowledgePoints()).extracting(TopicSequenceResult.KnowledgePointEntry::code)
                .containsExactly("LS_NUM", "LS_SPELL", "LS_PARA", "LS_TRAP");
        assertThat(listening.knowledgePoints()).allMatch(TopicSequenceResult.KnowledgePointEntry::hasPracticeSet);

        UUID ls1 = jdbc.queryForObject("SELECT id FROM lessons WHERE code = 'LS1'", Map.of(), UUID.class);
        LessonContentResult lesson = new GetLessonContentUseCase(reader, MEDIA).execute(ls1);
        assertThat(lesson.blocks()).extracting(LessonContentResult.Block::blockType)
                .containsExactly(BlockType.TEXT, BlockType.ASSET, BlockType.EXERCISE);
        LessonContentResult.Asset audio = lesson.blocks().get(1).asset();
        assertThat(audio.assetType()).isEqualTo(AssetType.AUDIO);
        assertThat(audio.mediaUrl()).isEqualTo("https://media.example.test/ieltspath/listening/demo/ls1.mp3");
        assertThat(audio.durationSeconds()).isEqualTo(45);
        assertThat(audio.textContent()).startsWith("Librarian: Good morning, Riverside Library.")
                .contains("Caller: Yes, it is Thompson. T, H, O, M, P, S, O, N.");
        LessonContentResult.Block exercise = lesson.blocks().get(2);
        assertThat(exercise.blockKind()).isEqualTo(LessonBlockKind.EXERCISE);
        assertThat(exercise.questions()).hasSize(4);
        JsonNode lq2 = JSON.readTree(exercise.questions().get(1).answerSpecJson());
        assertThat(grade(lq2, "  The   Fifteenth  ")).isTrue();

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
    void readingLessonHintsExistOnlyForEligibleQuestionsAndNeverGiveTheAnswer() throws Exception {
        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT qv.stem, qv.options::text AS options, qv.answer_spec::text AS spec, qv.hint
                FROM lesson_block_questions bq
                JOIN lesson_blocks b ON b.id = bq.block_id
                JOIN lessons l ON l.id = b.lesson_id
                JOIN topics t ON t.id = l.topic_id
                JOIN question_versions qv ON qv.id = bq.question_version_id
                WHERE t.code IN ('DEMO_READING', 'TFNG_SKILLS')
                """, Map.of());
        int hinted = 0;
        for (Map<String, Object> row : rows) {
            JsonNode spec = JSON.readTree((String) row.get("spec"));
            JsonNode options = row.get("options") == null ? null : JSON.readTree((String) row.get("options"));
            String hint = (String) row.get("hint");
            String type = spec.path("type").asText();
            boolean eligible = type.equals("FILL")
                    || (type.equals("CHOICE") && (options == null || options.isEmpty() || options.size() >= 3));
            String question = (String) row.get("stem");
            if (!eligible) {
                assertThat(hint).as("no hint for " + question).isNull();
                continue;
            }
            assertThat(hint).as("hint for " + question).isNotBlank().hasSizeLessThanOrEqualTo(500);
            hinted++;
            String normalizedHint = " " + normalize(hint).replaceAll("[^\\p{L}\\p{N}_ ]", " ") + " ";
            if (type.equals("FILL")) {
                for (JsonNode accepted : spec.get("accepted")) {
                    assertThat(normalizedHint).as(question).doesNotContain(" " + normalize(accepted.asText()) + " ");
                }
                continue;
            }
            String correct = spec.get("correct").asText();
            for (JsonNode option : options) {
                if (option.get("optionKey").asText().equals(correct)) {
                    assertThat(normalize(hint)).as(question).doesNotContain(normalize(option.get("content").asText()));
                }
            }
            if (correct.length() >= 2) {
                assertThat(normalizedHint).as(question).doesNotContain(" " + normalize(correct) + " ");
            }
            for (String verdict : List.of("true", "false", "not given", "not_given")) {
                assertThat(normalizedHint).as(question).doesNotContain(" " + verdict + " ");
            }
        }
        // Q13, Q1, Q11, Q12, Q5, Q4 and QT1; Q3 offers only two choices.
        assertThat(hinted).isEqualTo(7);
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
        // 43 reading questions, the Task 1 and Task 2 essays, 27 listening questions, the V4 practice-set question
        // the 10 questions of the paid topics and the 21 practice questions of V16.
        assertThat(reserved).hasSize(104);
        assertThat(reader.questionVersionsReservedForLearning(List.of(UUID.randomUUID()))).isEmpty();
    }

    // ---- helpers ----

    private static TopicSequenceResult topic(List<TopicSequenceResult> sequence, String code) {
        return sequence.stream().filter(topic -> topic.code().equals(code)).findFirst().orElseThrow();
    }

    private static UUID lessonId(String code) {
        return jdbc.queryForObject("SELECT id FROM lessons WHERE code = :code", Map.of("code", code), UUID.class);
    }

    private static List<UUID> blockIds(UUID lessonId) {
        return jdbc.queryForList("SELECT id FROM lesson_blocks WHERE lesson_id = :id ORDER BY sort_order",
                Map.of("id", lessonId), UUID.class);
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
