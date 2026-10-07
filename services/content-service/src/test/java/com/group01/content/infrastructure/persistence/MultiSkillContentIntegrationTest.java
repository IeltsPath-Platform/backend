package com.group01.content.infrastructure.persistence;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.group01.content.domain.vo.Skill;
import com.group01.content.application.result.LessonPracticeSetResult;
import com.group01.content.infrastructure.persistence.adapter.JdbcLearningContentReader;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;

import java.sql.Connection;
import java.util.UUID;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
class MultiSkillContentIntegrationTest {
    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");
    private static PGSimpleDataSource dataSource;
    private final ObjectMapper json = new ObjectMapper();
    private Connection connection;
    private JdbcTemplate sql;
    private JdbcLearningContentReader reader;
    private UUID topic;
    private UUID lesson;

    @BeforeAll
    static void migrate() {
        dataSource = new PGSimpleDataSource();
        dataSource.setURL(POSTGRES.getJdbcUrl());
        dataSource.setUser(POSTGRES.getUsername());
        dataSource.setPassword(POSTGRES.getPassword());
        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").load().migrate();
    }

    @BeforeEach
    void begin() throws Exception {
        connection = dataSource.getConnection();
        connection.setAutoCommit(false);
        var isolated = new SingleConnectionDataSource(connection, true);
        sql = new JdbcTemplate(isolated);
        reader = new JdbcLearningContentReader(new NamedParameterJdbcTemplate(isolated));
        topic = UUID.randomUUID();
        lesson = UUID.randomUUID();
        sql.update("INSERT INTO topics(id,code,name,sort_order,course_id) SELECT ?, 'MULTI_TEST', 'Mixed', 9999, id FROM courses WHERE code='IELTS_5_5'", topic);
        sql.update("INSERT INTO lessons(id,topic_id,code,title,sort_order,status) VALUES (?,?,'MULTI_LESSON','Mixed lesson',1,'PUBLISHED')", lesson, topic);
    }

    @AfterEach
    void rollback() throws Exception {
        connection.rollback();
        connection.close();
    }

    @Test
    void sequenceDerivesSkillsFromPublishedLessonsEvenWhenTopicSkillIsNull() {
        exercise("READING", 1);
        exercise("LISTENING", 2);
        var entry = reader.topicSequence(1).stream().filter(t -> t.topicId().equals(topic)).findFirst();
        assertThat(entry).isPresent();
        JsonNode payload = json.valueToTree(entry.orElseThrow());
        assertThat(payload.path("skills").toString()).isEqualTo("[\"LISTENING\",\"READING\"]");
        assertThat(payload.path("skill").isNull()).isTrue();
    }

    @Test
    void lessonAndSummaryIncludeTextKnowledgePointSkillsAndDeduplicateExerciseSkills() {
        exercise("READING", 1);
        exercise("READING", 2);
        UUID kp = UUID.randomUUID();
        UUID block = UUID.randomUUID();
        sql.update("INSERT INTO knowledge_points(id,topic_id,code,name,kind,learning_type,skill) VALUES (?,?,'MULTI_WRITING','Writing','STRATEGY','PROCEDURE','WRITING')", kp, topic);
        sql.update("INSERT INTO lesson_blocks(id,lesson_id,sort_order,block_type,text_content) VALUES (?,?,3,'TEXT','Writing theory')", block, lesson);
        sql.update("INSERT INTO lesson_block_knowledge_points(block_id,knowledge_point_id) VALUES (?,?)", block, kp);
        JsonNode detail = json.valueToTree(reader.publishedLesson(lesson).orElseThrow());
        JsonNode summary = json.valueToTree(reader.publishedLessons(topic).getFirst());
        assertThat(detail.path("skills").toString()).isEqualTo("[\"READING\",\"WRITING\"]");
        assertThat(summary.path("skills")).isEqualTo(detail.path("skills"));
        assertThat(detail.path("skill").isNull()).isTrue();
    }

    @Test
    void singleSkillCompatibilityComesFromExercisesRatherThanTopicMetadata() {
        exercise("LISTENING", 1);
        sql.update("UPDATE topics SET skill='READING' WHERE id=?", topic);
        assertThat(reader.publishedLesson(lesson).orElseThrow().skill().name()).isEqualTo("LISTENING");
        assertThat(reader.topicSequence(1).stream().filter(t -> t.topicId().equals(topic)).findFirst().orElseThrow().skill().name())
                .isEqualTo("LISTENING");
    }

    @Test
    void practiceFilterReturnsOnlyPureSkillSetsAndBothReadersExposeSkills() {
        UUID reading = practice("MULTI_R", "READING", "READING");
        UUID mixed = practice("MULTI_RL", "READING", "LISTENING");
        UUID writing = practice("MULTI_W", "WRITING");
        var filtered = reader.lessonPracticeSets(lesson, Optional.of(Skill.READING));
        assertThat(filtered).extracting(LessonPracticeSetResult::packageVersionId).containsExactly(reading);
        assertThat(filtered.getFirst().skills()).containsExactly(Skill.READING);

        var all = reader.lessonPracticeSets(lesson);
        assertThat(all).extracting(LessonPracticeSetResult::packageVersionId)
                .containsExactly(reading, mixed, writing);
        assertThat(all.get(1).skills()).containsExactly(Skill.LISTENING, Skill.READING);
        assertThat(all.get(2).skills()).containsExactly(Skill.WRITING);
        assertThat(reader.topicPracticeSets(topic)).singleElement().satisfies(group -> {
            assertThat(group.lessonId()).isEqualTo(lesson);
            assertThat(group.practiceSets()).isEqualTo(all);
        });
    }

    @Test
    void topicSkillsExcludeDraftLessonsAndAllKnowledgePoints() {
        exercise("READING", 1);
        UUID draft = UUID.randomUUID();
        sql.update("INSERT INTO lessons(id,topic_id,code,title,sort_order,status) VALUES (?,?,'MULTI_DRAFT','Draft lesson',2,'DRAFT')", draft, topic);
        exercise(draft, "WRITING", 1);
        UUID kp = UUID.randomUUID();
        UUID block = UUID.randomUUID();
        sql.update("INSERT INTO knowledge_points(id,topic_id,code,name,kind,learning_type,skill) VALUES (?,?,'MULTI_ALL','General','STRATEGY','PROCEDURE','ALL')", kp, topic);
        sql.update("INSERT INTO lesson_blocks(id,lesson_id,sort_order,block_type,text_content) VALUES (?,?,2,'TEXT','General theory')", block, lesson);
        sql.update("INSERT INTO lesson_block_knowledge_points(block_id,knowledge_point_id) VALUES (?,?)", block, kp);

        var entry = reader.topicSequence(1).stream().filter(t -> t.topicId().equals(topic)).findFirst().orElseThrow();
        assertThat(entry.skills()).containsExactly(Skill.READING);
        assertThat(entry.skill()).isEqualTo(Skill.READING);
        assertThat(reader.publishedLesson(lesson).orElseThrow().skills()).containsExactly(Skill.READING);
        assertThat(reader.publishedLessons(topic)).singleElement()
                .satisfies(summary -> assertThat(summary.skills()).containsExactly(Skill.READING));
    }

    @Test
    void practicePublishingAcceptsContainedSkillsAndRejectsSkillsOutsideLesson() {
        exercise("READING", 1);
        exercise("LISTENING", 2);
        UUID mixed = practice("MULTI_ALLOWED", "READING", "LISTENING");
        UUID outside = practice("MULTI_OUTSIDE", "READING", "WRITING");
        assertThat(reader.packageVersionLeavesLessonSkills(mixed, lesson)).isFalse();
        assertThat(reader.packageVersionLeavesLessonSkills(outside, lesson)).isTrue();
    }

    private UUID practice(String code, String... skills) {
        UUID packageId = UUID.randomUUID();
        UUID versionId = UUID.randomUUID();
        UUID sectionId = UUID.randomUUID();
        sql.update("INSERT INTO content_packages(id,code,title,package_type,status,lesson_id) VALUES (?,?,?,'PRACTICE_SET','PUBLISHED',?)", packageId, code, code, lesson);
        sql.update("INSERT INTO content_package_versions(id,package_id,version_number,status) VALUES (?,?,1,'PUBLISHED')", versionId, packageId);
        sql.update("UPDATE content_packages SET current_published_version_id=? WHERE id=?", versionId, packageId);
        sql.update("INSERT INTO content_sections(id,package_version_id,title,sort_order) VALUES (?,?,'Practice',1)", sectionId, versionId);
        for (int i = 0; i < skills.length; i++) {
            UUID question = UUID.randomUUID();
            UUID questionVersion = UUID.randomUUID();
            boolean essay = skills[i].equals("WRITING");
            sql.update("INSERT INTO questions(id,question_type,skill,status,purpose) VALUES (?,?,?,'PUBLISHED','LEARNING')", question, essay ? "ESSAY" : "MULTIPLE_CHOICE", skills[i]);
            String spec = essay ? "{\"type\":\"ESSAY\",\"task\":\"TASK_2\",\"passBand\":5.5}" : "{\"type\":\"CHOICE\",\"correct\":\"A\"}";
            sql.update("INSERT INTO question_versions(id,question_id,version_number,stem,answer_spec,status) VALUES (?,?,1,'Practice question',?::jsonb,'PUBLISHED')", questionVersion, question, spec);
            sql.update("INSERT INTO section_questions(section_id,question_version_id,sort_order) VALUES (?,?,?)", sectionId, questionVersion, i + 1);
        }
        return versionId;
    }

    private void exercise(String skill, int order) {
        exercise(lesson, skill, order);
    }

    private void exercise(UUID ownerLesson, String skill, int order) {
        UUID question = UUID.randomUUID();
        UUID version = UUID.randomUUID();
        UUID block = UUID.randomUUID();
        sql.update("INSERT INTO questions(id,question_type,skill,status,purpose) VALUES (?,'MULTIPLE_CHOICE',?,'PUBLISHED','LEARNING')", question, skill);
        sql.update("INSERT INTO question_versions(id,question_id,version_number,stem,answer_spec,status) VALUES (?,?,1,'Which statement is correct?','{\"type\":\"CHOICE\",\"correct\":\"A\"}'::jsonb,'PUBLISHED')", version, question);
        sql.update("INSERT INTO lesson_blocks(id,lesson_id,sort_order,block_type) VALUES (?,?,?,'EXERCISE')", block, ownerLesson, order);
        sql.update("INSERT INTO lesson_block_questions(block_id,question_version_id,sort_order) VALUES (?,?,1)", block, version);
    }
}
