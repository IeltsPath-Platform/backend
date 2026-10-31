package com.ieltspath.content.infrastructure.persistence;

import com.ieltspath.content.infrastructure.persistence.adapter.JdbcLearningContentReader;
import com.ieltspath.content.domain.vo.Skill;
import com.ieltspath.content.application.result.LessonSummaryResult;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
class TopicSequenceCourseMetadataIntegrationTest {
    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");
    private static JdbcLearningContentReader reader;
    private static NamedParameterJdbcTemplate jdbc;

    @BeforeAll
    static void migrate() {
        var source = new PGSimpleDataSource();
        source.setURL(POSTGRES.getJdbcUrl());
        source.setUser(POSTGRES.getUsername());
        source.setPassword(POSTGRES.getPassword());
        Flyway.configure().dataSource(source).locations("classpath:db/migration").target("88").load().migrate();
        jdbc = new NamedParameterJdbcTemplate(source);
        reader = new JdbcLearningContentReader(jdbc);
    }

    @Test
    void sequenceIncludesOptionalCourseCoverAndTopicDescriptions() {
        jdbc.getJdbcTemplate().update("UPDATE courses SET cover_image_url = ?, description = ? WHERE code = ?",
                "https://cdn.example/course.jpg", "Course overview", "IELTS_6_5");
        jdbc.getJdbcTemplate().update("UPDATE topics SET description = ? WHERE code = ?",
                "Build inference skills", "READING_6_5_INFERENCE");
        jdbc.getJdbcTemplate().update("UPDATE courses SET combo_unlock_points = ? WHERE code = ?", 600, "IELTS_6_5");
        jdbc.getJdbcTemplate().update("""
                INSERT INTO course_skill_prices(course_id, skill, unlock_points)
                SELECT id, 'READING', 250 FROM courses WHERE code = ?
                """, "IELTS_6_5");

        var topic = reader.topicSequence(3).stream()
                .filter(item -> item.code().equals("READING_6_5_INFERENCE"))
                .findFirst().orElseThrow();

        assertThat(topic.description()).isEqualTo("Build inference skills");
        assertThat(topic.course().coverImageUrl()).isEqualTo("https://cdn.example/course.jpg");
        assertThat(topic.course().description()).isEqualTo("Course overview");
        assertThat(topic.course().skillUnlockPoints()).containsEntry(Skill.READING, 250);
        assertThat(topic.course().comboUnlockPoints()).isEqualTo(600);
        assertThat(topic.course().availableSkills()).contains(Skill.READING);
        assertThat(topic.requiredFeatureKey()).isNull();
    }

    @Test
    void publishedLessonQueriesIncludeCourseSkillAndOrderedSkillPosition() {
        var topicId = jdbc.getJdbcTemplate().queryForObject("SELECT id FROM topics WHERE code = ?", java.util.UUID.class,
                "READING_6_5_INFERENCE");
        var first = jdbc.getJdbcTemplate().queryForObject("""
                SELECT id FROM lessons WHERE topic_id = ? AND status = 'PUBLISHED' ORDER BY sort_order, id LIMIT 1
                """, java.util.UUID.class, topicId);

        var summary = reader.publishedLessons(topicId).stream().filter(item -> item.lessonId().equals(first))
                .findFirst().orElseThrow();
        var lesson = reader.publishedLesson(first).orElseThrow();

        assertThat(summary.courseId()).isNotNull();
        assertThat(summary.skill()).isEqualTo(Skill.READING);
        assertThat(summary.skillPosition()).isEqualTo(1);
        assertThat(lesson.courseId()).isEqualTo(summary.courseId());
        assertThat(lesson.skill()).isEqualTo(Skill.READING);
        assertThat(lesson.skillPosition()).isEqualTo(1);
    }

    @Test
    void seededCourseSkillViolationsMatchTheContentAudit() {
        var unskilledTopics = jdbc.getJdbcTemplate().queryForList("""
                SELECT t.code FROM topics t JOIN courses c ON c.id = t.course_id WHERE t.skill IS NULL ORDER BY t.code
                """, String.class);
        var mismatchedPoints = jdbc.getJdbcTemplate().queryForList("""
                SELECT kp.code FROM knowledge_points kp JOIN topics t ON t.id = kp.topic_id
                JOIN courses c ON c.id = t.course_id
                WHERE kp.skill <> 'ALL' AND (t.skill IS NULL OR kp.skill <> t.skill) ORDER BY kp.code
                """, String.class);
        var multiskilledLessons = jdbc.getJdbcTemplate().queryForList("""
                SELECT t.code || '/' || l.code FROM lessons l JOIN topics t ON t.id = l.topic_id
                JOIN lesson_knowledge_points lkp ON lkp.lesson_id = l.id
                JOIN knowledge_points kp ON kp.id = lkp.knowledge_point_id AND kp.skill <> 'ALL'
                WHERE l.status = 'PUBLISHED'
                  AND t.course_id IS NOT NULL
                GROUP BY l.id, t.code, l.code HAVING count(DISTINCT kp.skill) > 1 ORDER BY t.code || '/' || l.code
                """, String.class);

        assertThat(unskilledTopics).containsExactly("TREES_MULTI_SKILL");
        assertThat(mismatchedPoints).containsExactly("TRAP_KEYWORD_MATCH_ONLY", "TR_L_DETAIL", "TR_R_SCANNING",
                "TR_R_WORD_IN_CONTEXT", "TR_W_OPINION");
        assertThat(multiskilledLessons).containsExactly("TREES_MULTI_SKILL/T1", "TREES_MULTI_SKILL/T2");
        assertThat(jdbc.getJdbcTemplate().queryForObject("SELECT count(*) FROM topics WHERE required_feature_key IS NOT NULL",
                Long.class)).isZero();
    }

    @Test
    void skillPositionsUseActiveTopicsAndPublishedLessonsAcrossTheCourse() {
        var sql = jdbc.getJdbcTemplate();
        var courseId = java.util.UUID.randomUUID();
        sql.update("INSERT INTO courses(id, code, name, band_level, status) VALUES (?, 'RANKING_TEST', 'Ranking test', 8.0, 'ACTIVE')",
                courseId);
        var firstTopic = topic(courseId, "RANKING_READING_A", Skill.READING, 10, "ACTIVE");
        var secondTopic = topic(courseId, "RANKING_READING_B", Skill.READING, 20, "ACTIVE");
        var inactiveTopic = topic(courseId, "RANKING_READING_INACTIVE", Skill.READING, 1, "INACTIVE");
        var listeningTopic = topic(courseId, "RANKING_LISTENING", Skill.LISTENING, 1, "ACTIVE");
        var freeTopic = topic(null, "RANKING_FREE", Skill.READING, 1, "ACTIVE");
        var first = lesson(firstTopic, "RANKING_A1", 1, "PUBLISHED");
        lesson(firstTopic, "RANKING_A_DRAFT", 2, "DRAFT");
        var second = lesson(firstTopic, "RANKING_A2", 3, "PUBLISHED");
        var third = lesson(firstTopic, "RANKING_A3", 4, "PUBLISHED");
        var fourth = lesson(secondTopic, "RANKING_B1", 1, "PUBLISHED");
        var fifth = lesson(secondTopic, "RANKING_B2", 2, "PUBLISHED");
        lesson(inactiveTopic, "RANKING_INACTIVE_LESSON", 1, "PUBLISHED");
        var listening = lesson(listeningTopic, "RANKING_LISTENING_LESSON", 1, "PUBLISHED");
        var free = lesson(freeTopic, "RANKING_FREE_LESSON", 1, "PUBLISHED");

        var pointId = java.util.UUID.randomUUID();
        sql.update("""
                INSERT INTO knowledge_points(id, topic_id, code, name, kind, learning_type, skill, status)
                VALUES (?, ?, 'RANKING_KP', 'Ranking point', 'STRATEGY', 'PROCEDURE', 'READING', 'ACTIVE')
                """, pointId, firstTopic);
        sql.update("INSERT INTO lesson_knowledge_points(lesson_id, knowledge_point_id) VALUES (?, ?)", second, pointId);
        sql.update("INSERT INTO lesson_knowledge_points(lesson_id, knowledge_point_id) VALUES (?, ?)", third, pointId);

        var readingPositions = java.util.stream.Stream.of(firstTopic, secondTopic)
                .flatMap(topicId -> reader.publishedLessons(topicId).stream())
                .map(LessonSummaryResult::skillPosition).sorted().toList();
        assertThat(readingPositions).containsExactly(1, 2, 3, 4, 5);
        assertThat(reader.publishedLessons(listeningTopic).getFirst().skillPosition()).isEqualTo(1);
        assertThat(reader.publishedLessons(freeTopic).getFirst()).satisfies(summary -> {
            assertThat(summary.courseId()).isNull();
            assertThat(summary.skillPosition()).isNull();
        });
        var sequenceTopic = reader.topicSequence(3).stream().filter(item -> item.topicId().equals(firstTopic))
                .findFirst().orElseThrow();
        assertThat(sequenceTopic.knowledgePoints()).filteredOn(point -> point.id().equals(pointId))
                .singleElement().satisfies(point -> assertThat(point.earliestLessonPosition()).isEqualTo(2));
        assertThat(reader.publishedLesson(listening).orElseThrow().skillPosition()).isEqualTo(1);
        assertThat(reader.publishedLesson(free).orElseThrow().skillPosition()).isNull();
    }

    private java.util.UUID topic(java.util.UUID courseId, String code, Skill skill, int order, String status) {
        var id = java.util.UUID.randomUUID();
        jdbc.getJdbcTemplate().update("""
                INSERT INTO topics(id, code, name, sort_order, status, skill, course_id)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, id, code, code, order, status, skill.name(), courseId);
        return id;
    }

    private java.util.UUID lesson(java.util.UUID topicId, String code, int order, String status) {
        var id = java.util.UUID.randomUUID();
        jdbc.getJdbcTemplate().update("""
                INSERT INTO lessons(id, topic_id, code, title, sort_order, status)
                VALUES (?, ?, ?, ?, ?, ?)
                """, id, topicId, code, code, order, status);
        return id;
    }
}
