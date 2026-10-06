package com.group01.content.infrastructure.persistence;

import com.group01.content.infrastructure.persistence.adapter.JdbcLearningContentReader;
import com.group01.content.domain.vo.PackageType;
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
        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").target("20").load().migrate();
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
        UUID low = course("LOW", "5.5", "ACTIVE");
        UUID high = course("HIGH", "6.5", "ACTIVE");
        UUID inactive = course("INACTIVE", "7.5", "INACTIVE");
        UUID lowerTopic = topic("LOWER", low, 100, true);
        UUID higherTopic = topic("HIGHER", high, 1, true);
        topic("UNASSIGNED", null, 0, true);
        topic("INACTIVE_COURSE", inactive, 0, true);
        topic("NO_LESSON", low, 0, false);
        var topics = reader.topicSequence(1);
        assertThat(topics).extracting(t -> t.topicId()).containsExactly(lowerTopic, higherTopic);
        assertThat(topics.getFirst().course().courseId()).isEqualTo(low);
        assertThat(topics.getFirst().course().bandLevel()).isEqualByComparingTo("5.5");
        assertThat(topics.getFirst().course().hasCourseTest()).isFalse();
        assertThat(topics.getLast().course().bandLevel()).isEqualByComparingTo("6.5");
    }

    @Test
    void courseTestFlagAndPackageQueryRequirePublishedPackagesAndCurrentVersions() {
        UUID course = course("LOW", "5.5", "ACTIVE");
        topic("TOPIC", course, 1, true);
        packageFixture("DRAFT", "COURSE_TEST", course, "DRAFT", "DRAFT", true);
        packageFixture("NO_CURRENT", "COURSE_TEST", course, "PUBLISHED", "PUBLISHED", false);
        assertThat(reader.topicSequence(1).getFirst().course().hasCourseTest()).isFalse();
        var published = packageFixture("FINAL", "COURSE_TEST", course, "PUBLISHED", "PUBLISHED", true);
        packageFixture("DRAFT_VERSION", "COURSE_TEST", course, "PUBLISHED", "DRAFT", true);
        assertThat(reader.topicSequence(1).getFirst().course().hasCourseTest()).isTrue();
        assertThat(reader.courseTestPackages(course)).hasSize(1);
        assertThat(reader.courseTestPackages(course).getFirst().packageVersionId()).isEqualTo(published.version());
        assertThat(reader.publishedPackageVersion(published.version()).orElseThrow().packageType()).isEqualTo(PackageType.COURSE_TEST);
    }

    @Test
    void courseQuestionsCannotBeExposedAsPracticeAndAreReservedAcrossQuestionVersions() {
        UUID course = course("LOW", "5.5", "ACTIVE");
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
        assertThat(reader.topicSequence(1).getFirst().knowledgePoints().getFirst().hasPracticeSet()).isFalse();
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
    void questionSpecProjectionIncludesEssayAndMalformedObjectiveShapesForPublishValidation() {
        UUID course = course("LOW", "5.5", "ACTIVE");
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

    private UUID course(String code, String band, String status) {
        UUID id = UUID.randomUUID();
        sql.update("INSERT INTO courses(id,code,name,band_level,status) VALUES (?,?,?,?::numeric,?)", id, code, code, band, status);
        return id;
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
