package com.ieltspath.content.infrastructure.persistence;

import com.ieltspath.content.application.result.TopicSequenceResult;
import com.ieltspath.content.domain.vo.PackageType;
import com.ieltspath.content.infrastructure.persistence.adapter.JdbcLearningContentReader;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.dao.DataIntegrityViolationException;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;

import java.sql.Connection;
import java.util.*;

import static org.assertj.core.api.Assertions.*;

@Testcontainers(disabledWithoutDocker = true)
class CourseSequenceIntegrationTest {
    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");
    private static PGSimpleDataSource dataSource;
    private Connection connection;
    private JdbcTemplate sql;
    private JdbcLearningContentReader reader;

    @BeforeAll
    static void migrate() {
        dataSource = new PGSimpleDataSource();
        dataSource.setURL(POSTGRES.getJdbcUrl());
        dataSource.setUser(POSTGRES.getUsername());
        dataSource.setPassword(POSTGRES.getPassword());
        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").target("52").load().migrate();
        try (Connection connection = dataSource.getConnection(); var statement = connection.createStatement()) {
            statement.executeUpdate("DELETE FROM course_practice_test_milestones");
            statement.executeUpdate("DELETE FROM content_packages WHERE course_id IN (SELECT id FROM courses)");
            statement.executeUpdate("UPDATE topics SET course_id = NULL WHERE course_id IN (SELECT id FROM courses)");
            statement.executeUpdate("DELETE FROM courses");
        } catch (Exception exception) {
            throw new IllegalStateException("Could not isolate course sequence fixtures", exception);
        }
    }

    @BeforeEach
    void begin() throws Exception {
        connection = dataSource.getConnection();
        connection.setAutoCommit(false);
        var isolated = new SingleConnectionDataSource(connection, true);
        sql = new JdbcTemplate(isolated);
        reader = new JdbcLearningContentReader(new NamedParameterJdbcTemplate(isolated));
    }

    @AfterEach
    void rollback() throws Exception {
        connection.rollback();
        connection.close();
    }

    @Test
    void sequenceExcludesUnassignedInactiveAndLessonlessTopicsAndUsesCourseBandOrder() {
        UUID low = course("LOW", "0.5", "ACTIVE");
        UUID high = course("HIGH", "1.0", "ACTIVE");
        UUID inactive = course("INACTIVE", "1.5", "INACTIVE");
        UUID lowerTopic = topic("LOWER", low, 100, true);
        UUID higherTopic = topic("HIGHER", high, 1, true);
        topic("UNASSIGNED", null, 0, true);
        topic("INACTIVE_COURSE", inactive, 0, true);
        topic("NO_LESSON", low, 0, false);
        var topics = reader.topicSequence(100).stream()
                .filter(candidate -> Set.of(low, high, inactive).contains(candidate.course().courseId()))
                .toList();
        assertThat(topics).extracting(t -> t.topicId()).containsExactly(lowerTopic, higherTopic);
        // Each topic holds one published lesson; the count is what the learner's course screen divides by.
        assertThat(topics).extracting(t -> t.lessonCount()).containsExactly(1, 1);
        assertThat(topics.getFirst().course().courseId()).isEqualTo(low);
        assertThat(topics.getFirst().course().bandLevel()).isEqualByComparingTo("0.5");
        assertThat(topics.getFirst().course().hasCourseTest()).isFalse();
        assertThat(topics.getLast().course().bandLevel()).isEqualByComparingTo("1.0");
    }

    @Test
    void courseTestFlagAndPackageQueryRequirePublishedPackagesAndCurrentVersions() {
        UUID course = course("LOW", "0.5", "ACTIVE");
        UUID topic = topic("TOPIC", course, 1, true);
        packageFixture("DRAFT", "COURSE_TEST", course, "DRAFT", "DRAFT", true);
        packageFixture("NO_CURRENT", "COURSE_TEST", course, "PUBLISHED", "PUBLISHED", false);
        assertThat(topicSequence(topic).course().hasCourseTest()).isFalse();
        var published = packageFixture("FINAL", "COURSE_TEST", course, "PUBLISHED", "PUBLISHED", true);
        packageFixture("DRAFT_VERSION", "COURSE_TEST", course, "PUBLISHED", "DRAFT", true);
        assertThat(topicSequence(topic).course().hasCourseTest()).isTrue();
        assertThat(reader.courseTestPackages(course)).hasSize(1);
        assertThat(reader.courseTestPackages(course).getFirst().packageVersionId()).isEqualTo(published.version());
        assertThat(reader.publishedPackageVersion(published.version()).orElseThrow().packageType()).isEqualTo(PackageType.COURSE_TEST);
    }

    @Test
    void courseQuestionsCannotBeExposedAsPracticeAndAreReservedAcrossQuestionVersions() {
        UUID course = course("LOW", "0.5", "ACTIVE");
        UUID topic = topic("TOPIC", course, 1, true);
        UUID kp = UUID.randomUUID();
        sql.update("INSERT INTO knowledge_points(id,topic_id,code,name,kind,learning_type,skill) VALUES (?,?,'KP','KP','STRATEGY','PROCEDURE','READING')", kp, topic);
        UUID question = UUID.randomUUID();
        UUID questionVersion = question(question, 1, "MULTIPLE_CHOICE", "{\"type\":\"CHOICE\",\"correct\":\"A\"}");
        sql.update("INSERT INTO question_knowledge_points(question_version_id,knowledge_point_id) VALUES (?,?)", questionVersion, kp);
        var practice = packageFixture("PRACTICE", "PRACTICE_SET", null, "PUBLISHED", "PUBLISHED", true);
        attach(practice.version(), questionVersion);
        assertThat(reader.searchPracticeSets(kp, List.of(), 1, 10, null)).extracting(p -> p.packageId()).contains(practice.pkg());
        var courseTest = packageFixture("FINAL", "COURSE_TEST", course, "PUBLISHED", "PUBLISHED", true);
        attach(courseTest.version(), questionVersion);
        assertThat(reader.searchPracticeSets(kp, List.of(), 1, 10, null)).isEmpty();
        assertThat(reader.countEligiblePracticeSets(List.of(kp), List.of(), 1).get(kp)).isZero();
        assertThat(topicSequence(topic).knowledgePoints()).filteredOn(point -> point.id().equals(kp))
                .singleElement().satisfies(point -> assertThat(point.hasPracticeSet()).isFalse());
        assertThat(reader.questionVersionsReservedForLearning(List.of(questionVersion))).contains(questionVersion);
        UUID courseOnlyQuestion = question(UUID.randomUUID(), 1, "FILL_IN_BLANK", "{\"type\":\"FILL\",\"accepted\":[\"answer\"]}");
        attach(courseTest.version(), courseOnlyQuestion);
        assertThat(reader.questionVersionsReservedForLearning(List.of(courseOnlyQuestion))).contains(courseOnlyQuestion);
        UUID newVersion = question(question, 2, "MULTIPLE_CHOICE", "{\"correct\":\"A\"}");
        var another = packageFixture("OTHER", "PRACTICE_SET", null, "DRAFT", "DRAFT", true);
        attach(another.version(), newVersion);
        assertThat(reader.questionsUsedElsewhere(another.version())).extracting(c -> c.ownerCode()).contains("FINAL");
    }

    @Test
    void practiceSetOrderPrefersLessonThenKnowledgePointMatchesThenPackageId() {
        UUID course = course("ORDER", "0.5", "ACTIVE");
        UUID topic = topic("ORDER_TOPIC", course, 1, true);
        UUID lesson = sql.queryForObject("SELECT id FROM lessons WHERE topic_id=?", UUID.class, topic);
        UUID kp = UUID.randomUUID();
        sql.update("INSERT INTO knowledge_points(id,topic_id,code,name,kind,learning_type,skill) " +
                "VALUES (?,?,'ORDER_KP','KP','STRATEGY','PROCEDURE','READING')", kp, topic);

        var preferred = packageFixture("ORDER_PREFERRED", "PRACTICE_SET", null, "PUBLISHED", "PUBLISHED", true);
        sql.update("UPDATE content_packages SET lesson_id=? WHERE id=?", lesson, preferred.pkg());
        var tiedA = packageFixture("ORDER_TIED_A", "PRACTICE_SET", null, "PUBLISHED", "PUBLISHED", true);
        var tiedB = packageFixture("ORDER_TIED_B", "PRACTICE_SET", null, "PUBLISHED", "PUBLISHED", true);
        var lowerMatches = packageFixture("ORDER_LOWER_MATCHES", "PRACTICE_SET", null, "PUBLISHED", "PUBLISHED", true);

        attachForOrder(preferred.version(), kp, 1);
        attachForOrder(tiedA.version(), kp, 2);
        attachForOrder(tiedB.version(), kp, 2);
        attachForOrder(lowerMatches.version(), kp, 1);

        var results = reader.searchPracticeSets(kp, List.of(), 2, 10, lesson);
        var tieSorted = List.of(tiedA, tiedB).stream()
                .sorted(Comparator.comparing(fixture -> fixture.pkg().toString()))
                .map(fixture -> fixture.pkg().toString()).toList();
        assertThat(results).extracting(result -> result.packageId().toString())
                .containsExactly(preferred.pkg().toString(), tieSorted.get(0), tieSorted.get(1), lowerMatches.pkg().toString());
    }

    private void attachForOrder(UUID version, UUID knowledgePointId, int matchedQuestions) {
        for (int index = 0; index < 2; index++) {
            UUID questionVersion = question(UUID.randomUUID(), 1, "MULTIPLE_CHOICE", "{\"type\":\"CHOICE\",\"correct\":\"A\"}");
            if (index < matchedQuestions) {
                sql.update("INSERT INTO question_knowledge_points(question_version_id,knowledge_point_id) VALUES (?,?)",
                        questionVersion, knowledgePointId);
            }
            attach(version, questionVersion);
        }
    }

    @Test
    void questionSpecProjectionIncludesEssayAndMalformedObjectiveShapesForPublishValidation() {
        UUID course = course("LOW", "0.5", "ACTIVE");
        var courseTest = packageFixture("FINAL", "COURSE_TEST", course, "DRAFT", "DRAFT", true);
        UUID questionVersion = question(UUID.randomUUID(), 1, "ESSAY", "{\"type\":\"ESSAY\"}");
        attach(courseTest.version(), questionVersion);
        var question = reader.packageQuestionSpecs(courseTest.version()).getFirst();
        assertThat(question.questionVersionId()).isEqualTo(questionVersion);
        assertThat(question.questionType().name()).isEqualTo("ESSAY");
        assertThat(question.skill().name()).isEqualTo("READING");
        assertThat(question.answerSpecJson()).contains("ESSAY");
    }

    @Test
    void courseTestsRequireAnExistingCourseAndCannotOmitCourseMembership() {
        assertThatThrownBy(() -> packageFixture("NO_COURSE", "COURSE_TEST", null, "DRAFT", "DRAFT", false))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void courseTestForeignKeyRejectsUnknownCourses() {
        assertThatThrownBy(() -> packageFixture("UNKNOWN_COURSE", "COURSE_TEST", UUID.randomUUID(), "DRAFT", "DRAFT", false))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void progressMetadataUsesPublishedDenominatorAndRevisionChangesWithMembership() {
        UUID course = course("STATS", "5.5", "ACTIVE");
        UUID topic = topic("STATS_TOPIC", course, 1, true);
        UUID lesson = sql.queryForObject("SELECT id FROM lessons WHERE topic_id=?", UUID.class, topic);
        UUID block = UUID.randomUUID(), kp = UUID.randomUUID();
        sql.update("INSERT INTO knowledge_points(id,topic_id,code,name,kind,learning_type,skill) VALUES (?,?,'STATS_KP','KP','STRATEGY','PROCEDURE','LISTENING')",kp,topic);
        sql.update("INSERT INTO lesson_blocks(id,lesson_id,sort_order,block_type,text_content) VALUES (?,?,1,'TEXT','Theory')",block,lesson);
        sql.update("INSERT INTO lesson_block_knowledge_points(block_id,knowledge_point_id) VALUES (?,?)",block,kp);
        var first = reader.progressMetadata(List.of(course));
        assertThat(first.complete()).isTrue();
        assertThat(first.lessons()).hasSize(1);
        assertThat(first.lessons().getFirst().skills()).containsExactly(com.ieltspath.content.domain.vo.Skill.LISTENING);
        assertThat(first.revision()).isEqualTo(reader.progressMetadata(List.of(course)).revision());
        sql.update("UPDATE lessons SET status='ARCHIVED' WHERE id=?",lesson);
        var removed = reader.progressMetadata(List.of(course));
        assertThat(removed.lessons()).isEmpty();
        assertThat(removed.revision()).isNotEqualTo(first.revision());
    }

    @Test
    void progressMetadataRejectsOversizedDenominatorWithoutReturningPartialRows() {
        UUID course = course("STATS_LARGE", "5.5", "ACTIVE");
        UUID topic = topic("STATS_LARGE_TOPIC", course, 1, false);
        sql.update("INSERT INTO lessons(id,topic_id,code,title,sort_order,status) SELECT gen_random_uuid(),?, 'STATS_' || n,'Lesson',n,'PUBLISHED' FROM generate_series(1,10001) n",topic);
        var result = reader.progressMetadata(List.of(course));
        assertThat(result.complete()).isFalse();
        assertThat(result.lessons()).isEmpty();
    }

    private UUID course(String code, String band, String status) {
        UUID id = UUID.randomUUID();
        sql.update("INSERT INTO courses(id,code,name,band_level,status) VALUES (?,?,?,?::numeric,?)", id, code, code, band, status);
        return id;
    }

    private TopicSequenceResult topicSequence(UUID topicId) {
        return reader.topicSequence(100).stream().filter(candidate -> candidate.topicId().equals(topicId))
                .findFirst().orElseThrow();
    }

    private UUID topic(String code, UUID course, int order, boolean lesson) {
        UUID id = UUID.randomUUID();
        sql.update("INSERT INTO topics(id,code,name,sort_order,skill,course_id) VALUES (?,?,?,?,'READING',?)", id, code, code, order, course);
        if (lesson) sql.update("INSERT INTO lessons(id,topic_id,code,title,sort_order,status) VALUES (?,?,?,?,1,'PUBLISHED')",
                UUID.randomUUID(), id, "LESSON_" + code, code);
        return id;
    }

    private record PackageFixture(UUID pkg, UUID version) {}

    private PackageFixture packageFixture(String code, String type, UUID course, String status, String versionStatus, boolean current) {
        UUID pkg = UUID.randomUUID();
        UUID version = UUID.randomUUID();
        sql.update("INSERT INTO content_packages(id,code,title,package_type,status,course_id) VALUES (?,?,?,?,?,?)", pkg, code, code, type, status, course);
        sql.update("INSERT INTO content_package_versions(id,package_id,version_number,status) VALUES (?,?,1,?)", version, pkg, versionStatus);
        if (current) sql.update("UPDATE content_packages SET current_published_version_id=? WHERE id=?", version, pkg);
        return new PackageFixture(pkg, version);
    }

    private UUID question(UUID question, int number, String type, String spec) {
        if (number == 1) sql.update("INSERT INTO questions(id,question_type,skill,status,purpose) VALUES (?,?,'READING','PUBLISHED','LEARNING')", question, type);
        UUID version = UUID.randomUUID();
        sql.update("INSERT INTO question_versions(id,question_id,version_number,stem,answer_spec,status) VALUES (?,?,?,'Question',?::jsonb,'PUBLISHED')",
                version, question, number, spec);
        return version;
    }

    private void attach(UUID version, UUID questionVersion) {
        var sections = sql.queryForList("SELECT id FROM content_sections WHERE package_version_id=? ORDER BY sort_order", UUID.class, version);
        UUID section;
        if (sections.isEmpty()) {
            section = UUID.randomUUID();
            sql.update("INSERT INTO content_sections(id,package_version_id,title,skill,sort_order) VALUES (?,?,'Section','READING',1)", section, version);
        } else {
            section = sections.getFirst();
        }
        int order = sql.queryForObject("SELECT count(*) FROM section_questions WHERE section_id=?", Integer.class, section) + 1;
        sql.update("INSERT INTO section_questions(id,section_id,question_version_id,sort_order) VALUES (?,?,?,?)", UUID.randomUUID(), section, questionVersion, order);
    }
}
