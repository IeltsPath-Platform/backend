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

@Testcontainers(disabledWithoutDocker = true)
class CourseSeedTest {
    @Container private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");
    private static NamedParameterJdbcTemplate jdbc;
    private static JdbcLearningContentReader reader;

    @BeforeAll
    static void migrate() {
        var source = new PGSimpleDataSource();
        source.setURL(POSTGRES.getJdbcUrl());
        source.setUser(POSTGRES.getUsername());
        source.setPassword(POSTGRES.getPassword());
        Flyway.configure().dataSource(source).locations("classpath:db/migration").target("21").load().migrate();
        jdbc = new NamedParameterJdbcTemplate(source);
        reader = new JdbcLearningContentReader(jdbc);
    }

    @Test
    void everySkilledTopicBelongsToACourseAndHigherReadingFollowsLowerReading() {
        assertThat(jdbc.queryForObject("SELECT count(*) FROM topics WHERE skill IS NOT NULL AND course_id IS NULL", Map.of(), Integer.class)).isZero();
        var reading = reader.topicSequence(3).stream().filter(topic -> topic.skill() == Skill.READING).toList();
        assertThat(reading.getLast().code()).isEqualTo("READING_6_5_INFERENCE");
        assertThat(reading.getLast().course().bandLevel()).isEqualByComparingTo("6.5");
        assertThat(reading.subList(0, reading.size() - 1)).allMatch(topic -> topic.course().bandLevel().doubleValue() == 5.5);
        assertThat(reader.topicSequence(3)).allMatch(topic -> topic.course().hasCourseTest());
    }

    @Test
    void higherBandLessonTeachesBothKnowledgePointsAndOffersTheirPracticeSets() {
        TopicSequenceResult topic = reader.topicSequence(3).stream().filter(t -> t.code().equals("READING_6_5_INFERENCE")).findFirst().orElseThrow();
        assertThat(topic.knowledgePoints()).hasSize(2).allMatch(TopicSequenceResult.KnowledgePointEntry::hasPracticeSet);
        var lesson = reader.publishedLessons(topic.topicId()).getFirst();
        assertThat(lesson.knowledgePointIds()).hasSize(2);
        var content = reader.publishedLesson(lesson.lessonId()).orElseThrow();
        assertThat(content.blocks().stream().filter(block -> block.blockType() == BlockType.TEXT).findFirst().orElseThrow().knowledgePointIds()).hasSize(2);
        assertThat(reader.lessonPracticeSets(lesson.lessonId())).hasSize(2).allMatch(set -> set.questionCount() >= 3);
        assertThat(reader.publishedTestPackages(topic.topicId())).hasSize(1);
    }

    @Test
    void bothCourseTestsHaveNewObjectiveReadingQuestionsAndNoOtherOwner() {
        var courses = jdbc.queryForList("SELECT id FROM courses ORDER BY band_level", Map.of(), UUID.class);
        assertThat(courses).hasSize(2);
        for (UUID course : courses) {
            var packages = reader.courseTestPackages(course);
            assertThat(packages).hasSize(1);
            var test = packages.getFirst();
            var payload = reader.publishedPackageVersion(test.packageVersionId()).orElseThrow();
            assertThat(payload.packageType()).isEqualTo(PackageType.COURSE_TEST);
            assertThat(payload.sections()).singleElement().satisfies(section -> {
                assertThat(section.skill()).isEqualTo(Skill.READING);
                assertThat(section.items()).hasSizeBetween(6, 10);
            });
            assertThat(reader.questionsUsedElsewhere(test.packageVersionId())).isEmpty();
            assertThat(reader.questionsWithWrongPurpose(test.packageVersionId(), QuestionPurpose.LEARNING)).isEmpty();
            assertThat(reader.packageQuestionSpecs(test.packageVersionId())).allMatch(question -> question.skill() == Skill.READING && question.questionType() != QuestionType.ESSAY);
        }
    }

    @Test
    void everyNewQuestionHasAStructurallyValidObjectiveAnswerSpecification() throws Exception {
        var questions = jdbc.queryForList("SELECT q.question_type,qv.answer_spec::text AS spec FROM questions q JOIN question_versions qv ON qv.question_id=q.id WHERE q.id::text LIKE '28000000-%'", Map.of());
        assertThat(questions).isNotEmpty();
        var json = new ObjectMapper();
        for (var question : questions) {
            var spec = json.readTree((String) question.get("spec"));
            if ("FILL".equals(spec.path("type").asText())) {
                assertThat(question.get("question_type")).isEqualTo("FILL_IN_BLANK");
                assertThat(spec.path("accepted").isArray()).isTrue();
                assertThat(spec.path("accepted").isEmpty()).isFalse();
            } else {
                assertThat(spec.path("type").asText()).isEqualTo("CHOICE");
                assertThat(question.get("question_type")).isEqualTo("MULTIPLE_CHOICE");
                assertThat(spec.path("correct").asText()).isNotBlank();
            }
        }
    }
}
