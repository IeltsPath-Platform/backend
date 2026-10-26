package com.ieltspath.content.infrastructure.persistence;

import org.flywaydb.core.Flyway;
import com.ieltspath.content.infrastructure.persistence.adapter.JdbcLearningContentReader;
import com.ieltspath.content.application.result.TopicSequenceResult;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
class ErrorTrapKnowledgePointMigrationTest {
    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");
    private static NamedParameterJdbcTemplate jdbc;
    private static JdbcLearningContentReader reader;

    @BeforeAll
    static void migrate() {
        var source = new PGSimpleDataSource();
        source.setURL(POSTGRES.getJdbcUrl());
        source.setUser(POSTGRES.getUsername());
        source.setPassword(POSTGRES.getPassword());
        Flyway.configure().dataSource(source).locations("classpath:db/migration").target("79").load().migrate();
        jdbc = new NamedParameterJdbcTemplate(source);
        reader = new JdbcLearningContentReader(jdbc);
    }

    @Test
    void everyActiveErrorTypeWithActiveParentHasOneMatchingTrapPoint() {
        var rows = jdbc.queryForList("""
                SELECT et.code, et.trap_knowledge_point_id, parent.id AS parent_id,
                       parent.topic_id AS parent_topic, parent.kind AS parent_kind,
                       parent.learning_type AS parent_learning_type, parent.skill AS parent_skill,
                       child.id AS child_id, child.code AS child_code, child.topic_id AS child_topic,
                       child.kind AS child_kind, child.learning_type AS child_learning_type, child.skill AS child_skill
                FROM error_types et
                JOIN knowledge_points parent ON parent.id = et.knowledge_point_id
                LEFT JOIN knowledge_points child ON child.id = et.trap_knowledge_point_id
                WHERE et.status = 'ACTIVE' AND parent.status = 'ACTIVE'
                ORDER BY et.code
                """, Collections.emptyMap());

        assertThat(rows).isNotEmpty().allSatisfy(row -> {
            assertThat(row.get("trap_knowledge_point_id")).isNotNull();
            assertThat(row.get("child_id")).isEqualTo(row.get("trap_knowledge_point_id"));
            assertThat(row.get("child_code")).isEqualTo("TRAP_" + row.get("code"));
            assertThat(row.get("child_topic")).isEqualTo(row.get("parent_topic"));
            assertThat(row.get("child_kind")).isEqualTo(row.get("parent_kind"));
            assertThat(row.get("child_learning_type")).isEqualTo(row.get("parent_learning_type"));
            assertThat(row.get("child_skill")).isEqualTo(row.get("parent_skill"));
        });

        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM error_types et
                LEFT JOIN knowledge_points parent ON parent.id = et.knowledge_point_id
                WHERE et.trap_knowledge_point_id IS NOT NULL
                  AND (et.status <> 'ACTIVE' OR parent.id IS NULL OR parent.status <> 'ACTIVE')
                """, Collections.emptyMap(), Long.class)).isZero();
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM (
                    SELECT trap_knowledge_point_id FROM error_types
                    WHERE trap_knowledge_point_id IS NOT NULL
                    GROUP BY trap_knowledge_point_id HAVING count(*) > 1
                ) duplicate_trap_points
                """, Collections.emptyMap(), Long.class)).isZero();
    }

    @Test
    void parentKnowledgePointIsUnchanged() {
        assertThat(jdbc.queryForObject("""
                SELECT parent.code FROM error_types et
                JOIN knowledge_points parent ON parent.id = et.knowledge_point_id
                WHERE et.code = 'LINKER_MISUSE'
                """, Collections.emptyMap(), String.class)).isEqualTo("DEMO_WRITING_LINKING");
    }

    @Test
    void notGivenQuestionsGetTheFalseTrap() {
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM question_versions qv
                JOIN questions q ON q.id = qv.question_id
                WHERE q.question_type = 'TRUE_FALSE_NOT_GIVEN'
                  AND qv.status = 'PUBLISHED'
                  AND qv.answer_spec->>'correct' = 'NOT_GIVEN'
                  AND (qv.options @> '[{"optionKey":"FALSE"}]' OR qv.options @> '[{"optionKey":"NO"}]')
                  AND qv.answer_spec->'distractors'->>CASE
                      WHEN qv.options @> '[{"optionKey":"NO"}]' THEN 'NO' ELSE 'FALSE' END = 'TFNG_NG_AS_FALSE'
                """, Collections.emptyMap(), Long.class)).isGreaterThan(0);
    }

    @Test
    void falseQuestionsGetTheNotGivenTrap() {
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM question_versions qv
                JOIN questions q ON q.id = qv.question_id
                WHERE q.question_type = 'TRUE_FALSE_NOT_GIVEN'
                  AND qv.status = 'PUBLISHED'
                  AND qv.answer_spec->>'correct' IN ('FALSE', 'NO')
                  AND qv.options @> '[{"optionKey":"NOT_GIVEN"}]'
                  AND qv.answer_spec->'distractors'->>'NOT_GIVEN' = 'TFNG_FALSE_AS_NG'
                """, Collections.emptyMap(), Long.class)).isGreaterThan(0);
    }

    @Test
    void trueQuestionsGetNoTrap() {
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM question_versions qv
                JOIN questions q ON q.id = qv.question_id
                WHERE q.question_type = 'TRUE_FALSE_NOT_GIVEN'
                  AND qv.status = 'PUBLISHED'
                  AND qv.answer_spec->>'correct' IN ('TRUE', 'YES')
                  AND jsonb_exists(qv.answer_spec, 'distractors')
                """, Collections.emptyMap(), Long.class)).isZero();
    }

    @Test
    void publishedTrueFalseNotGivenCorrectAnswerCountsAreUnchanged() {
        Map<String, Long> counts = jdbc.query("""
                SELECT qv.answer_spec->>'correct' AS correct, count(*) AS questions
                FROM question_versions qv
                JOIN questions q ON q.id = qv.question_id
                WHERE q.question_type = 'TRUE_FALSE_NOT_GIVEN' AND qv.status = 'PUBLISHED'
                GROUP BY qv.answer_spec->>'correct'
                """, Collections.emptyMap(), rs -> {
            var result = new java.util.HashMap<String, Long>();
            while (rs.next()) result.put(rs.getString("correct"), rs.getLong("questions"));
            return result;
        });

        assertThat(counts).containsExactlyInAnyOrderEntriesOf(Map.of(
                "FALSE", 18L, "NO", 4L, "NOT_GIVEN", 21L, "TRUE", 19L, "YES", 2L));
    }

    @Test
    void topicSequenceDoesNotListTrapPoints() {
        var linkedTrapIds = jdbc.queryForList(
                        "SELECT trap_knowledge_point_id FROM error_types WHERE trap_knowledge_point_id IS NOT NULL",
                        Collections.emptyMap())
                .stream().map(row -> (UUID) row.get("trap_knowledge_point_id")).toList();

        assertThat(reader.topicSequence(3).stream()
                .flatMap(topic -> topic.knowledgePoints().stream())
                .map(point -> point.id()))
                .doesNotContainAnyElementsOf(linkedTrapIds);
    }

    @Test
    void topicSequenceKeepsAuthorKnowledgePointsWhoseCodeStartsWithTrap() {
        var sourcePoint = reader.topicSequence(3).stream()
                .flatMap(topic -> topic.knowledgePoints().stream())
                .findFirst()
                .orElseThrow();
        UUID authorPointId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO knowledge_points (id, topic_id, code, name, kind, learning_type, skill, status)
                SELECT :id, topic_id, 'TRAP_AUTHOR_DEFINED', 'Author-authored trap topic', kind, learning_type, skill, 'ACTIVE'
                FROM knowledge_points WHERE id = :sourcePointId
                """, Map.of("id", authorPointId, "sourcePointId", sourcePoint.id()));

        var defaultPoints = reader.topicSequence(3).stream()
                .flatMap(topic -> topic.knowledgePoints().stream()).toList();
        var expandedPoints = reader.topicSequence(3, true).stream()
                .flatMap(topic -> topic.knowledgePoints().stream()).toList();

        assertThat(defaultPoints).extracting(point -> point.id()).contains(authorPointId);
        assertThat(expandedPoints).filteredOn(point -> point.id().equals(authorPointId)).singleElement()
                .satisfies(point -> {
                    assertThat(point.parentKnowledgePointId()).isNull();
                    assertThat(point.errorCode()).isNull();
                });
    }

    @Test
    void topicSequenceIncludesTrapPointsOnlyWhenRequested() {
        List<TopicSequenceResult.KnowledgePointEntry> traps = reader.topicSequence(3, true).stream()
                .flatMap(topic -> topic.knowledgePoints().stream())
                .filter(point -> point.parentKnowledgePointId() != null)
                .toList();

        assertThat(traps).isNotEmpty().allSatisfy(point -> {
            assertThat(point.parentKnowledgePointId()).isNotNull();
            assertThat(point.errorCode()).isEqualTo(point.code().substring("TRAP_".length()));
            assertThat(point.hasPracticeSet()).isFalse();
        });
    }

    @Test
    void practiceSetSearchRanksByRequestedTrapBeforeLegacyLessonPreference() {
        UUID knowledgePointId = jdbc.queryForObject(
                "SELECT id FROM knowledge_points WHERE code = 'TFNG_FALSE_VS_NOT_GIVEN'", Collections.emptyMap(), UUID.class);
        UUID lessonId = jdbc.queryForObject("SELECT id FROM lessons ORDER BY id LIMIT 1", Collections.emptyMap(), UUID.class);
        UUID setA = addPracticeSet("TRAP_SEARCH_A", null, knowledgePointId,
                List.of("NOT_GIVEN", "NOT_GIVEN", "NOT_GIVEN", "NOT_GIVEN", "FALSE"));
        UUID setB = addPracticeSet("TRAP_SEARCH_B", null, knowledgePointId,
                List.of("NOT_GIVEN", "FALSE", "FALSE", "FALSE", "FALSE"));
        UUID setC = addPracticeSet("TRAP_SEARCH_C", lessonId, knowledgePointId,
                List.of("NOT_GIVEN", "NOT_GIVEN", "FALSE", "FALSE", "FALSE"));
        UUID setD = addPracticeSet("TRAP_SEARCH_D", null, knowledgePointId,
                List.of("NOT_GIVEN", "FALSE", "FALSE"));
        UUID setE = addPracticeSet("TRAP_SEARCH_E", lessonId, knowledgePointId,
                List.of("NOT_GIVEN", "FALSE", "FALSE"));

        assertThat(reader.searchPracticeSets(knowledgePointId, List.of(), 3, 10, lessonId, null))
                .extracting(result -> result.packageId()).first().isEqualTo(setC);
        assertThat(reader.searchPracticeSets(knowledgePointId, List.of(), 3, 10, lessonId, null)
                .stream().map(result -> result.packageId()).toList()).contains(setA, setB);

        var notGivenTrap = reader.searchPracticeSets(knowledgePointId, List.of(), 3, 10, lessonId,
                "TFNG_NG_AS_FALSE");
        assertThat(notGivenTrap).extracting(result -> result.packageId()).containsSubsequence(setA, setC, setB);
        assertThat(notGivenTrap).filteredOn(result -> result.packageId().equals(setA)).singleElement()
                .extracting(result -> result.trapQuestionCount()).isEqualTo(4);
        List<UUID> notGivenOrder = notGivenTrap.stream().map(result -> result.packageId()).toList();
        assertThat(notGivenOrder.indexOf(setE)).isLessThan(notGivenOrder.indexOf(setD));

        var falseTrap = reader.searchPracticeSets(knowledgePointId, List.of(), 3, 10, lessonId,
                "TFNG_FALSE_AS_NG");
        assertThat(falseTrap).extracting(result -> result.packageId()).containsSubsequence(setB, setC, setA);
        assertThat(falseTrap).filteredOn(result -> result.packageId().equals(setB)).singleElement()
                .extracting(result -> result.trapQuestionCount()).isEqualTo(4);
        assertThat(reader.searchPracticeSets(knowledgePointId, List.of(), 3, 10, lessonId, null))
                .allMatch(result -> result.trapQuestionCount() == 0);
    }

    private UUID addPracticeSet(String code, UUID lessonId, UUID knowledgePointId, List<String> correctAnswers) {
        UUID packageId = UUID.nameUUIDFromBytes(code.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        UUID versionId = UUID.randomUUID();
        UUID sectionId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO content_packages (id, code, title, package_type, status)
                VALUES (:id, :code, :title, 'PRACTICE_SET', 'PUBLISHED')
                """, Map.of("id", packageId, "code", code, "title", code));
        if (lessonId != null) {
            jdbc.update("UPDATE content_packages SET lesson_id = :lessonId WHERE id = :id",
                    Map.of("id", packageId, "lessonId", lessonId));
        }
        jdbc.update("""
                INSERT INTO content_package_versions (id, package_id, version_number, status, published_at)
                VALUES (:id, :packageId, 1, 'PUBLISHED', CURRENT_TIMESTAMP)
                """, Map.of("id", versionId, "packageId", packageId));
        jdbc.update("UPDATE content_packages SET current_published_version_id = :versionId WHERE id = :id",
                Map.of("id", packageId, "versionId", versionId));
        jdbc.update("""
                INSERT INTO content_sections (id, package_version_id, title, skill, sort_order)
                VALUES (:id, :versionId, 'Trap search', 'READING', 1)
                """, Map.of("id", sectionId, "versionId", versionId));

        for (int i = 0; i < correctAnswers.size(); i++) {
            String correct = correctAnswers.get(i);
            UUID questionId = UUID.randomUUID();
            UUID questionVersionId = UUID.randomUUID();
            jdbc.update("""
                    INSERT INTO questions (id, question_type, skill, purpose, status)
                    VALUES (:id, 'TRUE_FALSE_NOT_GIVEN', 'READING', 'LEARNING', 'PUBLISHED')
                    """, Map.of("id", questionId));
            String distractorKey = "NOT_GIVEN".equals(correct) ? "FALSE" : "NOT_GIVEN";
            String errorCode = "NOT_GIVEN".equals(correct) ? "TFNG_NG_AS_FALSE" : "TFNG_FALSE_AS_NG";
            String options = """
                    [{"optionKey":"TRUE"},{"optionKey":"FALSE"},{"optionKey":"NOT_GIVEN"}]
                    """;
            String answerSpec = """
                    {"correct":"%s","distractors":{"%s":"%s"}}
                    """.formatted(correct, distractorKey, errorCode);
            jdbc.update("""
                    INSERT INTO question_versions (id, question_id, version_number, stem, options, answer_spec,
                                                   explanation, difficulty, status)
                    VALUES (:id, :questionId, 1, 'A locally written test statement.', CAST(:options AS jsonb),
                            CAST(:answerSpec AS jsonb), 'The selected option conflicts with the text.', 'EASY', 'PUBLISHED')
                    """, Map.of("id", questionVersionId, "questionId", questionId, "options", options,
                    "answerSpec", answerSpec));
            jdbc.update("UPDATE questions SET current_published_version_id = :versionId WHERE id = :id",
                    Map.of("versionId", questionVersionId, "id", questionId));
            jdbc.update("""
                    INSERT INTO section_questions (section_id, question_version_id, sort_order)
                    VALUES (:sectionId, :questionVersionId, :sortOrder)
                    """, Map.of("sectionId", sectionId, "questionVersionId", questionVersionId,
                    "sortOrder", i + 1));
            jdbc.update("""
                    INSERT INTO question_knowledge_points (question_version_id, knowledge_point_id, weight)
                    VALUES (:questionVersionId, :knowledgePointId, 1.00)
                    """, Map.of("questionVersionId", questionVersionId, "knowledgePointId", knowledgePointId));
        }
        return packageId;
    }
}
