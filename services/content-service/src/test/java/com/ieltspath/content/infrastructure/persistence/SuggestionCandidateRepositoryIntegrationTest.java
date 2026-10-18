package com.ieltspath.content.infrastructure.persistence;

import com.ieltspath.content.application.result.SuggestionCandidatesResult;
import com.ieltspath.content.domain.vo.PackageType;
import com.ieltspath.content.infrastructure.persistence.adapter.JdbcSuggestionCandidateRepository;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class SuggestionCandidateRepositoryIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");
    private static PGSimpleDataSource dataSource;

    private JdbcTemplate jdbc;
    private DataSourceTransactionManager transactionManager;
    private TransactionStatus transaction;
    private JdbcSuggestionCandidateRepository repository;
    private UUID lowKp;
    private UUID highKp;
    private UUID lowLesson;
    private UUID highLesson;
    private PackageFixture lowSet;
    private PackageFixture lowTest;
    private PackageFixture highTest;

    @BeforeAll
    static void migrate() {
        dataSource = new PGSimpleDataSource();
        dataSource.setURL(POSTGRES.getJdbcUrl());
        dataSource.setUser(POSTGRES.getUsername());
        dataSource.setPassword(POSTGRES.getPassword());
        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").load().migrate();
    }

    @BeforeEach
    void setUp() {
        jdbc = new JdbcTemplate(dataSource);
        transactionManager = new DataSourceTransactionManager(dataSource);
        transaction = transactionManager.getTransaction(new DefaultTransactionDefinition());
        repository = new JdbcSuggestionCandidateRepository(new NamedParameterJdbcTemplate(dataSource));

        UUID lowCourse = jdbc.queryForObject("SELECT id FROM courses WHERE band_level=5.5", UUID.class);
        UUID highCourse = jdbc.queryForObject("SELECT id FROM courses WHERE band_level=6.5", UUID.class);
        UUID lowTopic = topic(lowCourse);
        UUID highTopic = topic(highCourse);
        lowKp = knowledgePoint(lowTopic);
        highKp = knowledgePoint(highTopic);
        lowLesson = lesson(lowTopic, lowKp);
        highLesson = lesson(highTopic, highKp);
        lowSet = practicePackage("PRACTICE_SET", lowKp, null, lowLesson);
        lowTest = practicePackage("PRACTICE_TEST", lowKp, lowCourse, null);
        highTest = practicePackage("PRACTICE_TEST", highKp, highCourse, null);
    }

    @AfterEach
    void rollback() {
        if (transaction != null && !transaction.isCompleted()) transactionManager.rollback(transaction);
    }

    @Test
    void batchesSetsTestsAndLessonsWithinTheRequestedBand() {
        var atLowBand = candidates(new BigDecimal("5.5"));
        assertThat(atLowBand.packages()).extracting(SuggestionCandidatesResult.PackageCandidate::packageId)
                .containsExactlyInAnyOrder(lowSet.id(), lowTest.id());
        assertThat(atLowBand.packages()).extracting(SuggestionCandidatesResult.PackageCandidate::type)
                .containsExactlyInAnyOrder(PackageType.PRACTICE_SET, PackageType.PRACTICE_TEST);
        assertThat(atLowBand.packages()).allSatisfy(item -> {
            assertThat(item.packageVersionId()).isNotNull();
            assertThat(item.knowledgePointIds()).containsExactly(lowKp);
            assertThat(item.band()).isEqualByComparingTo("5.5");
            assertThat(item.skill().name()).isEqualTo("READING");
        });
        assertThat(atLowBand.lessons()).extracting(SuggestionCandidatesResult.LessonCandidate::lessonId)
                .containsExactly(lowLesson);

        var atHighBand = candidates(new BigDecimal("6.5"));
        assertThat(atHighBand.packages()).extracting(SuggestionCandidatesResult.PackageCandidate::packageId)
                .containsExactlyInAnyOrder(lowSet.id(), lowTest.id(), highTest.id());
        assertThat(atHighBand.lessons()).extracting(SuggestionCandidatesResult.LessonCandidate::lessonId)
                .containsExactly(lowLesson, highLesson);
        assertThat(atHighBand.lessons()).allSatisfy(item -> {
            assertThat(item.knowledgePointIds()).hasSize(1);
            assertThat(item.sortOrder()).isEqualTo(1);
        });
        assertThat(repository.packages(List.of(UUID.randomUUID()), new BigDecimal("9.0"))).isEmpty();
    }

    @Test
    void hidesUnpublishedPackageVersionAndLesson() {
        jdbc.update("UPDATE content_packages SET status='ARCHIVED' WHERE id=?", lowSet.id());
        jdbc.update("UPDATE content_package_versions SET status='DRAFT' WHERE id=?", lowTest.versionId());
        jdbc.update("UPDATE lessons SET status='DRAFT' WHERE id=?", lowLesson);

        var result = repository.packages(List.of(lowKp), new BigDecimal("5.5"));
        assertThat(result).isEmpty();
        assertThat(repository.lessons(List.of(lowKp), new BigDecimal("5.5"))).isEmpty();
    }

    @Test
    void usesTheCurrentVersionAndTheHighestMappedKnowledgePointBand() {
        jdbc.update("INSERT INTO question_knowledge_points(question_version_id,knowledge_point_id) VALUES (?,?)",
                lowSet.questionVersionId(), highKp);
        jdbc.update("UPDATE knowledge_points SET skill='WRITING' WHERE id=?", highKp);
        var lowBand = candidates(new BigDecimal("5.5"));
        assertThat(lowBand.packages()).extracting(SuggestionCandidatesResult.PackageCandidate::packageId)
                .containsExactly(lowTest.id());

        var highBand = candidates(new BigDecimal("6.5"));
        var set = highBand.packages().stream().filter(item -> item.packageId().equals(lowSet.id())).findFirst().orElseThrow();
        assertThat(set.band()).isEqualByComparingTo("6.5");
        assertThat(set.knowledgePointIds()).containsExactlyInAnyOrder(lowKp, highKp);
        assertThat(set.skill()).isNull();

        jdbc.update("UPDATE content_packages SET current_published_version_id=? WHERE id=?",
                highTest.versionId(), lowSet.id());
        assertThat(repository.packages(List.of(lowKp), new BigDecimal("6.5")))
                .extracting(SuggestionCandidatesResult.PackageCandidate::packageId).doesNotContain(lowSet.id());
    }

    @Test
    void inactiveKnowledgePointTopicOrCourseCannotBeSuggested() {
        assertThat(repository.packages(List.of(lowKp), new BigDecimal("9.0"))).hasSize(2);
        jdbc.update("INSERT INTO lesson_knowledge_points(lesson_id,knowledge_point_id) VALUES (?,?)",
                lowLesson, highKp);
        jdbc.update("UPDATE knowledge_points SET status='INACTIVE' WHERE id=?", lowKp);
        assertThat(repository.packages(List.of(lowKp), new BigDecimal("9.0"))).isEmpty();
        assertThat(repository.lessons(List.of(lowKp), new BigDecimal("9.0"))).isEmpty();

        jdbc.update("UPDATE knowledge_points SET status='ACTIVE' WHERE id=?", lowKp);
        jdbc.update("UPDATE topics SET status='INACTIVE' WHERE id=(SELECT topic_id FROM knowledge_points WHERE id=?)",
                lowKp);
        assertThat(repository.packages(List.of(lowKp), new BigDecimal("9.0"))).isEmpty();
        assertThat(repository.lessons(List.of(lowKp), new BigDecimal("9.0"))).isEmpty();

        jdbc.update("UPDATE topics SET status='ACTIVE' WHERE id=(SELECT topic_id FROM knowledge_points WHERE id=?)",
                lowKp);
        jdbc.update("UPDATE courses SET status='INACTIVE' WHERE id=(SELECT t.course_id FROM topics t "
                + "JOIN knowledge_points kp ON kp.topic_id=t.id WHERE kp.id=?)", lowKp);
        assertThat(repository.packages(List.of(lowKp), new BigDecimal("9.0"))).isEmpty();
        assertThat(repository.lessons(List.of(lowKp), new BigDecimal("9.0"))).isEmpty();
    }

    @Test
    void excludesPackagesWhoseLessonOrCourseOwnerIsInactive() {
        jdbc.update("UPDATE lessons SET status='DRAFT' WHERE id=?", lowLesson);
        assertThat(repository.packages(List.of(lowKp), new BigDecimal("9.0")))
                .extracting(SuggestionCandidatesResult.PackageCandidate::packageId).containsExactly(lowTest.id());

        jdbc.update("UPDATE lessons SET status='PUBLISHED' WHERE id=?", lowLesson);
        UUID otherCourse = jdbc.queryForObject("SELECT id FROM courses WHERE band_level=6.5", UUID.class);
        jdbc.update("UPDATE content_packages SET course_id=? WHERE id=?", otherCourse, lowTest.id());
        jdbc.update("UPDATE courses SET status='INACTIVE' WHERE id=?", otherCourse);
        assertThat(repository.packages(List.of(lowKp), new BigDecimal("9.0")))
                .extracting(SuggestionCandidatesResult.PackageCandidate::packageId).containsExactly(lowSet.id());
    }

    private SuggestionCandidatesResult candidates(BigDecimal maxBand) {
        List<UUID> ids = List.of(lowKp, highKp);
        return new SuggestionCandidatesResult(repository.packages(ids, maxBand), repository.lessons(ids, maxBand));
    }

    private UUID topic(UUID course) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO topics(id,code,name,sort_order,status,course_id) VALUES (?,?,?,1,'ACTIVE',?)",
                id, "SUGG_TOPIC_" + id, "Suggestion topic", course);
        return id;
    }

    private UUID knowledgePoint(UUID topic) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO knowledge_points(id,topic_id,code,name,kind,skill,learning_type,status) "
                        + "VALUES (?,?,?,?,'STRATEGY','READING','PROCEDURE','ACTIVE')",
                id, topic, "SUGG_KP_" + id, "Suggestion KP");
        return id;
    }

    private UUID lesson(UUID topic, UUID knowledgePoint) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO lessons(id,topic_id,code,title,sort_order,status) "
                        + "VALUES (?,?,?,? ,1,'PUBLISHED')",
                id, topic, "SUGG_LESSON_" + id, "Suggestion lesson");
        jdbc.update("INSERT INTO lesson_knowledge_points(lesson_id,knowledge_point_id) VALUES (?,?)",
                id, knowledgePoint);
        return id;
    }

    private PackageFixture practicePackage(String type, UUID knowledgePoint, UUID course, UUID lesson) {
        UUID packageId = UUID.randomUUID();
        UUID versionId = UUID.randomUUID();
        UUID sectionId = UUID.randomUUID();
        UUID questionId = UUID.randomUUID();
        UUID questionVersionId = UUID.randomUUID();
        jdbc.update("INSERT INTO content_packages(id,code,title,package_type,status,course_id,lesson_id) "
                        + "VALUES (?,?,?,?,'PUBLISHED',?,?)",
                packageId, "SUGG_PACKAGE_" + packageId, "Suggestion package", type, course, lesson);
        jdbc.update("INSERT INTO content_package_versions(id,package_id,version_number,status) "
                        + "VALUES (?,?,1,'PUBLISHED')", versionId, packageId);
        jdbc.update("UPDATE content_packages SET current_published_version_id=? WHERE id=?", versionId, packageId);
        jdbc.update("INSERT INTO content_sections(id,package_version_id,title,skill,sort_order) "
                        + "VALUES (?,?,'Section','READING',1)", sectionId, versionId);
        jdbc.update("INSERT INTO questions(id,question_type,skill,status) "
                        + "VALUES (?,'MULTIPLE_CHOICE','READING','PUBLISHED')", questionId);
        jdbc.update("INSERT INTO question_versions(id,question_id,version_number,stem,status) "
                        + "VALUES (?,?,1,'Question','PUBLISHED')", questionVersionId, questionId);
        jdbc.update("INSERT INTO section_questions(section_id,question_version_id,sort_order) VALUES (?,?,1)",
                sectionId, questionVersionId);
        jdbc.update("INSERT INTO question_knowledge_points(question_version_id,knowledge_point_id) VALUES (?,?)",
                questionVersionId, knowledgePoint);
        return new PackageFixture(packageId, versionId, questionVersionId);
    }

    private record PackageFixture(UUID id, UUID versionId, UUID questionVersionId) {}
}
