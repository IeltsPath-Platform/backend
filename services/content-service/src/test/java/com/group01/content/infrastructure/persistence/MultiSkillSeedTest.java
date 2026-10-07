package com.group01.content.infrastructure.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.group01.content.application.result.LessonContentResult;
import com.group01.content.application.result.LessonSummaryResult;
import com.group01.content.application.result.TopicSequenceResult;
import com.group01.content.domain.vo.*;
import com.group01.content.infrastructure.persistence.adapter.JdbcLearningContentReader;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

/** The seeded multi-skill topic: lessons that teach Reading, Listening and Writing together. */
@Testcontainers(disabledWithoutDocker = true)
class MultiSkillSeedTest {
    private static final List<Skill> ALL_THREE = List.of(Skill.LISTENING, Skill.READING, Skill.WRITING);

    @Container private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");
    private static NamedParameterJdbcTemplate jdbc;
    private static JdbcLearningContentReader reader;

    @BeforeAll
    static void migrate() {
        var source = new PGSimpleDataSource();
        source.setURL(POSTGRES.getJdbcUrl());
        source.setUser(POSTGRES.getUsername());
        source.setPassword(POSTGRES.getPassword());
        Flyway.configure().dataSource(source).locations("classpath:db/migration").target("23").load().migrate();
        jdbc = new NamedParameterJdbcTemplate(source);
        reader = new JdbcLearningContentReader(jdbc);
    }

    @Test
    void theTopicJoinsTheLowerCourseAndTeachesThreeSkills() {
        TopicSequenceResult topic = topic();
        assertThat(topic.course().code()).isEqualTo("IELTS_5_5");
        assertThat(topic.skills()).containsExactlyElementsOf(ALL_THREE);
        assertThat(topic.skill()).isNull();
        assertThat(topic.hasTopicTest()).isTrue();
        assertThat(topic.knowledgePoints()).extracting(TopicSequenceResult.KnowledgePointEntry::skill)
                .containsExactlyInAnyOrder(Skill.READING, Skill.READING, Skill.LISTENING, Skill.WRITING);
    }

    @Test
    void bothLessonsTeachEverySkillAndEachTheoryBlockNamesItsKnowledgePoints() {
        List<LessonSummaryResult> lessons = reader.publishedLessons(topic().topicId());
        assertThat(lessons).extracting(LessonSummaryResult::code).containsExactly("T1", "T2");
        for (LessonSummaryResult lesson : lessons) {
            assertThat(lesson.skills()).as(lesson.code()).containsExactlyElementsOf(ALL_THREE);
            LessonContentResult content = reader.publishedLesson(lesson.lessonId()).orElseThrow();
            assertThat(content.skills()).containsExactlyElementsOf(ALL_THREE);
            assertThat(content.blocks()).filteredOn(block -> block.blockType() == BlockType.TEXT).hasSize(3)
                    .allSatisfy(block -> assertThat(block.knowledgePointIds()).isNotEmpty());
        }
    }

    @Test
    void theFirstLessonOffersSingleSkillAndMixedPractice() {
        UUID t1 = reader.publishedLessons(topic().topicId()).getFirst().lessonId();
        assertThat(reader.lessonPracticeSets(t1)).hasSize(4);
        assertThat(reader.lessonPracticeSets(t1, Optional.of(Skill.WRITING))).singleElement()
                .satisfies(set -> assertThat(set.questionCount()).isEqualTo(1));
        assertThat(reader.lessonPracticeSets(t1, Optional.of(Skill.READING))).singleElement()
                .satisfies(set -> assertThat(set.questionCount()).isGreaterThanOrEqualTo(3));
        assertThat(reader.lessonPracticeSets(t1, Optional.of(Skill.LISTENING))).singleElement()
                .satisfies(set -> assertThat(set.questionCount()).isGreaterThanOrEqualTo(3));
        var mixed = reader.lessonPracticeSets(t1).stream()
                .filter(set -> set.skills().equals(List.of(Skill.LISTENING, Skill.READING))).toList();
        assertThat(mixed).singleElement();
        Map<String, Integer> perSkill = new HashMap<>();
        jdbc.query("""
                SELECT s.skill, count(*) AS questions FROM content_sections s
                JOIN section_questions sq ON sq.section_id = s.id
                WHERE s.package_version_id = :version GROUP BY s.skill
                """, Map.of("version", mixed.getFirst().packageVersionId()),
                rs -> { perSkill.put(rs.getString("skill"), rs.getInt("questions")); });
        assertThat(perSkill).containsOnlyKeys("READING", "LISTENING").allSatisfy((skill, count) ->
                assertThat(count).isGreaterThanOrEqualTo(3));
    }

    @Test
    void theTopicTestMixesReadingListeningAndOneEssay() {
        var tests = reader.publishedTestPackages(topic().topicId());
        assertThat(tests).hasSize(1);
        var payload = reader.publishedPackageVersion(tests.getFirst().packageVersionId()).orElseThrow();
        assertThat(payload.sections()).extracting(section -> section.skill())
                .containsExactly(Skill.READING, Skill.LISTENING, Skill.WRITING);
        assertThat(reader.packageQuestionSpecs(tests.getFirst().packageVersionId()))
                .filteredOn(question -> question.questionType() == QuestionType.ESSAY).hasSize(1);
    }

    @Test
    void everyEssayHasAPassBandAndNoQuestionHasTwoOwners() throws Exception {
        var json = new ObjectMapper();
        List<String> essays = jdbc.queryForList("""
                SELECT qv.answer_spec::text FROM question_versions qv JOIN questions q ON q.id = qv.question_id
                WHERE q.id::text LIKE '2a000000-%' AND q.question_type = 'ESSAY'
                """, Map.of(), String.class);
        assertThat(essays).hasSize(4);
        for (String essay : essays) {
            assertThat(json.readTree(essay).path("passBand").isNumber()).as(essay).isTrue();
        }
        Integer shared = jdbc.queryForObject("""
                SELECT count(*) FROM (
                    SELECT question_version_id FROM (
                        SELECT sq.question_version_id, s.package_version_id::text AS owner FROM section_questions sq
                        JOIN content_sections s ON s.id = sq.section_id
                        UNION ALL
                        SELECT bq.question_version_id, b.lesson_id::text FROM lesson_block_questions bq
                        JOIN lesson_blocks b ON b.id = bq.block_id) owners
                    WHERE question_version_id::text LIKE '2a000000-%'
                    GROUP BY question_version_id HAVING count(DISTINCT owner) > 1) duplicated
                """, Map.of(), Integer.class);
        assertThat(shared).isZero();
    }

    private static TopicSequenceResult topic() {
        return reader.topicSequence(3).stream().filter(topic -> topic.code().equals("TREES_MULTI_SKILL"))
                .findFirst().orElseThrow();
    }
}
