package com.group01.content.infrastructure.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
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

/** Demo content that lets every course show all three skills and rotate between two final tests. */
@Testcontainers(disabledWithoutDocker = true)
class CourseDemoContentSeedTest {
    @Container private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");
    private static NamedParameterJdbcTemplate jdbc;
    private static JdbcLearningContentReader reader;

    @BeforeAll
    static void migrate() {
        var source = new PGSimpleDataSource();
        source.setURL(POSTGRES.getJdbcUrl());
        source.setUser(POSTGRES.getUsername());
        source.setPassword(POSTGRES.getPassword());
        Flyway.configure().dataSource(source).locations("classpath:db/migration").target("22").load().migrate();
        jdbc = new NamedParameterJdbcTemplate(source);
        reader = new JdbcLearningContentReader(jdbc);
    }

    @Test
    void everyCourseCoversReadingListeningAndWriting() {
        var sequence = reader.topicSequence(3);
        for (String course : List.of("IELTS_5_5", "IELTS_6_5")) {
            assertThat(sequence.stream().filter(topic -> topic.course().code().equals(course)).map(TopicSequenceResult::skill))
                    .as(course).contains(Skill.READING, Skill.LISTENING, Skill.WRITING);
        }
        var higher = sequence.stream().filter(topic -> topic.course().code().equals("IELTS_6_5"))
                .sorted(Comparator.comparingInt(TopicSequenceResult::sortOrder)).map(TopicSequenceResult::code).toList();
        assertThat(higher).containsExactly("READING_6_5_INFERENCE", "LISTENING_6_5_DETAIL", "WRITING_6_5_DISCUSSION");
    }

    @Test
    void everyCourseHasTwoObjectiveFinalTestsWithUnsharedQuestions() {
        var courses = jdbc.queryForList("SELECT id FROM courses ORDER BY band_level", Map.of(), UUID.class);
        for (UUID course : courses) {
            var packages = reader.courseTestPackages(course);
            assertThat(packages).hasSize(2);
            for (var test : packages) {
                var payload = reader.publishedPackageVersion(test.packageVersionId()).orElseThrow();
                assertThat(payload.packageType()).isEqualTo(PackageType.COURSE_TEST);
                assertThat(payload.sections()).allSatisfy(section -> assertThat(section.items()).hasSizeBetween(6, 10));
                assertThat(reader.questionsUsedElsewhere(test.packageVersionId())).isEmpty();
                assertThat(reader.packageQuestionSpecs(test.packageVersionId()))
                        .allMatch(question -> question.questionType() != QuestionType.ESSAY);
            }
        }
    }

    @Test
    void higherListeningTopicHasPracticeForEachKnowledgePointAndAFinalTestWithAudio() {
        var topic = topic("LISTENING_6_5_DETAIL");
        assertThat(topic.skill()).isEqualTo(Skill.LISTENING);
        assertThat(topic.knowledgePoints()).hasSize(2).allMatch(TopicSequenceResult.KnowledgePointEntry::hasPracticeSet);
        var lesson = reader.publishedLessons(topic.topicId()).getFirst();
        assertThat(reader.lessonPracticeSets(lesson.lessonId())).hasSize(2).allMatch(set -> set.questionCount() >= 3);
        var tests = reader.publishedTestPackages(topic.topicId());
        assertThat(tests).hasSize(1);
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM content_asset_links l JOIN content_sections s ON s.id = l.section_id
                JOIN content_assets a ON a.id = l.asset_id
                WHERE s.package_version_id = :version AND a.asset_type = 'AUDIO'
                """, Map.of("version", tests.getFirst().packageVersionId()), Integer.class)).isEqualTo(1);
    }

    @Test
    void higherWritingTopicTeachesATask2EssayGradedAtTheCourseBand() throws Exception {
        var topic = topic("WRITING_6_5_DISCUSSION");
        assertThat(topic.skill()).isEqualTo(Skill.WRITING);
        assertThat(reader.publishedTestPackages(topic.topicId())).isEmpty();
        String spec = jdbc.queryForObject("""
                SELECT qv.answer_spec::text FROM lesson_block_questions bq
                JOIN lesson_blocks b ON b.id = bq.block_id JOIN lessons l ON l.id = b.lesson_id
                JOIN question_versions qv ON qv.id = bq.question_version_id
                WHERE l.topic_id = :topic
                """, Map.of("topic", topic.topicId()), String.class);
        var essay = new ObjectMapper().readTree(spec);
        assertThat(essay.path("type").asText()).isEqualTo("ESSAY");
        assertThat(essay.path("task").asText()).isEqualTo("TASK_2");
        assertThat(essay.path("passBand").decimalValue()).isEqualByComparingTo("6.5");
    }

    private static TopicSequenceResult topic(String code) {
        return reader.topicSequence(3).stream().filter(topic -> topic.code().equals(code)).findFirst().orElseThrow();
    }
}
