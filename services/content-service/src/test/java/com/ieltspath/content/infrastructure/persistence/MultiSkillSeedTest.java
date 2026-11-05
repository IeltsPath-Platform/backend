package com.ieltspath.content.infrastructure.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.content.application.result.LessonContentResult;
import com.ieltspath.content.application.result.LessonSummaryResult;
import com.ieltspath.content.application.result.TopicSequenceResult;
import com.ieltspath.content.domain.vo.*;
import com.ieltspath.content.infrastructure.persistence.adapter.JdbcLearningContentReader;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

/** The tree course keeps its reading, listening and writing content in separate topics. */
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
        Flyway.configure().dataSource(source).locations("classpath:db/migration").target("92").load().migrate();
        jdbc = new NamedParameterJdbcTemplate(source);
        reader = new JdbcLearningContentReader(jdbc);
    }

    @Test
    void eachTreeTopicHasOneSkillAndItsOwnTopicTest() {
        assertThat(reader.topicSequence(3).stream().filter(topic -> topic.code().startsWith("TREES_")))
                .extracting(TopicSequenceResult::code, TopicSequenceResult::skill, topic -> topic.course().code(),
                        TopicSequenceResult::hasTopicTest)
                .containsExactlyInAnyOrder(
                        tuple("TREES_READING", Skill.READING, "IELTS_5_5", true),
                        tuple("TREES_LISTENING", Skill.LISTENING, "IELTS_5_5", true),
                        tuple("TREES_WRITING", Skill.WRITING, "IELTS_5_5", true));
        assertThat(jdbc.queryForList("SELECT code FROM topics WHERE code LIKE 'TREES_%' AND skill IS NULL", Map.of(), String.class))
                .isEmpty();
    }

    @Test
    void splitLessonsTeachOnlyTheirTopicsSkillAndKeepTheoryKnowledgePoints() {
        for (var skill : ALL_THREE) {
            TopicSequenceResult topic = topic(skill);
            List<LessonSummaryResult> lessons = reader.publishedLessons(topic.topicId());
            assertThat(lessons).hasSize(2).allSatisfy(lesson -> {
                assertThat(lesson.skill()).isEqualTo(skill);
                assertThat(lesson.skills()).containsExactly(skill);
                LessonContentResult content = reader.publishedLesson(lesson.lessonId()).orElseThrow();
                assertThat(content.skill()).isEqualTo(skill);
                assertThat(content.skills()).containsExactly(skill);
                assertThat(content.blocks()).filteredOn(block -> block.blockType() == BlockType.TEXT)
                        .allSatisfy(block -> assertThat(block.knowledgePointIds()).isNotEmpty());
            });
        }
    }

    @Test
    void skillPracticeSetsStayWithTheirLessonsAndMixedSetIsRetired() {
        assertThat(reader.lessonPracticeSets(lesson("T1"))).extracting(set -> set.code())
                .containsExactly("TR-PS-READING");
        assertThat(reader.lessonPracticeSets(lesson("T1_L"))).extracting(set -> set.code())
                .containsExactly("TR-PS-LISTENING");
        assertThat(reader.lessonPracticeSets(lesson("T1_W"))).extracting(set -> set.code())
                .containsExactly("TR-PS-WRITING");
        assertThat(jdbc.queryForObject("SELECT status FROM content_packages WHERE code = 'TR-PS-MIXED'", Map.of(), String.class))
                .isEqualTo("ARCHIVED");
    }

    @Test
    void topicTestsContainOnlyTheTopicSkillAndKeepQuestionIdentities() {
        for (var skill : ALL_THREE) {
            var tests = reader.publishedTestPackages(topic(skill).topicId());
            assertThat(tests).hasSize(1);
            var payload = reader.publishedPackageVersion(tests.getFirst().packageVersionId()).orElseThrow();
            assertThat(payload.sections()).extracting(section -> section.skill()).containsExactly(skill);
            assertThat(reader.packageQuestionSpecs(tests.getFirst().packageVersionId())).isNotEmpty();
        }
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

    private static TopicSequenceResult topic(Skill skill) {
        return reader.topicSequence(3).stream().filter(topic -> topic.code().equals("TREES_" + skill.name()))
                .findFirst().orElseThrow();
    }

    private static UUID lesson(String code) {
        return reader.topicSequence(3).stream().filter(topic -> topic.code().startsWith("TREES_"))
                .flatMap(topic -> reader.publishedLessons(topic.topicId()).stream())
                .filter(lesson -> lesson.code().equals(code)).map(LessonSummaryResult::lessonId).findFirst().orElseThrow();
    }
}
