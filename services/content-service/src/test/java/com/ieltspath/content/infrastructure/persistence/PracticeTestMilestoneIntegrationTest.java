package com.ieltspath.content.infrastructure.persistence;

import com.ieltspath.content.application.usecase.GetPracticeTestMilestonesUseCase;
import com.ieltspath.content.application.usecase.ReplacePracticeTestMilestonesUseCase;
import com.ieltspath.content.domain.aggregate.Course;
import com.ieltspath.content.domain.exception.CourseNotFoundException;
import com.ieltspath.content.domain.exception.InvalidContentStateException;
import com.ieltspath.content.domain.repository.CourseRepository;
import com.ieltspath.content.domain.vo.ContentStatus;
import com.ieltspath.content.domain.vo.PracticeTestMilestone;
import com.ieltspath.content.infrastructure.persistence.adapter.JdbcPracticeTestMilestoneRepository;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Testcontainers
class PracticeTestMilestoneIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");
    private static PGSimpleDataSource dataSource;
    private JdbcTemplate jdbc;
    private DataSourceTransactionManager transactionManager;
    private TransactionStatus isolation;
    private TransactionTemplate transaction;
    private JdbcPracticeTestMilestoneRepository repository;
    private GetPracticeTestMilestonesUseCase get;
    private ReplacePracticeTestMilestonesUseCase replace;
    private Course course;

    @BeforeAll
    static void migrate() {
        dataSource = new PGSimpleDataSource();
        dataSource.setURL(POSTGRES.getJdbcUrl());
        dataSource.setUser(POSTGRES.getUsername());
        dataSource.setPassword(POSTGRES.getPassword());
        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").load().migrate();
    }

    @BeforeEach
    void begin() {
        jdbc = new JdbcTemplate(dataSource);
        transactionManager = new DataSourceTransactionManager(dataSource);
        isolation = transactionManager.getTransaction(new DefaultTransactionDefinition());
        transaction = new TransactionTemplate(transactionManager);
        // Savepoints let a failed replacement roll back before we inspect the original rows.
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_NESTED);
        repository = new JdbcPracticeTestMilestoneRepository(new NamedParameterJdbcTemplate(dataSource));
        CourseRepository courses = mock(CourseRepository.class);
        course = Course.create("MILESTONE_TEST", "Milestone test", new BigDecimal("8.5"));
        jdbc.update("INSERT INTO courses(id,code,name,band_level,status) VALUES (?,?,?,?,'ACTIVE')",
                course.getId(), course.getCode(), course.getName(), course.getBandLevel());
        when(courses.findById(course.getId())).thenReturn(Optional.of(course));
        get = new GetPracticeTestMilestonesUseCase(courses, repository);
        replace = new ReplacePracticeTestMilestonesUseCase(courses, repository);
        topic("FIRST", 10, "ACTIVE", "PUBLISHED");
        topic("SECOND", 30, "ACTIVE", "PUBLISHED");
        topic("INACTIVE", 40, "INACTIVE", "PUBLISHED");
        topic("LESSONLESS", 50, "ACTIVE", null);
        topic("DRAFT_LESSON", 60, "ACTIVE", "DRAFT");
    }

    @AfterEach
    void rollback() {
        if (isolation != null && !isolation.isCompleted()) transactionManager.rollback(isolation);
    }

    @Test
    void seedsPublishedPracticeTestsAtReachableMidCourseMilestonesForBothBands() {
        var rows = jdbc.queryForList("""
                SELECT c.band_level, c.status course_status, m.after_topic_sequence,
                       p.package_type, p.status, v.status version_status, v.package_id = p.id owns_version,
                       (SELECT count(*) FROM topics t WHERE t.course_id=c.id AND t.status='ACTIVE'
                         AND EXISTS (SELECT 1 FROM lessons l WHERE l.topic_id=t.id AND l.status='PUBLISHED')) topics
                FROM course_practice_test_milestones m JOIN courses c ON c.id=m.course_id
                JOIN content_packages p ON p.id=m.package_id
                LEFT JOIN content_package_versions v ON v.id=p.current_published_version_id
                WHERE c.band_level IN (5.5, 6.5)
                ORDER BY c.band_level
                """);
        assertThat(rows).extracting(row -> (BigDecimal) row.get("band_level"))
                .contains(new BigDecimal("5.5"), new BigDecimal("6.5"));
        for (var row : rows) {
            assertThat(row.get("course_status")).isEqualTo("ACTIVE");
            assertThat(row.get("package_type")).isEqualTo("PRACTICE_TEST");
            assertThat(row.get("status")).isEqualTo("PUBLISHED");
            assertThat(row.get("version_status")).isEqualTo("PUBLISHED");
            assertThat(row.get("owns_version")).isEqualTo(true);
            assertThat(((Number) row.get("after_topic_sequence")).intValue())
                    .isPositive().isLessThan(((Number) row.get("topics")).intValue());
        }
    }

    @Test
    void assessmentCanReadSeededPracticeVersionsAndTheirQuestionsRemainReserved() {
        var reader = new com.ieltspath.content.infrastructure.persistence.adapter.JdbcLearningContentReader(
                new NamedParameterJdbcTemplate(dataSource));
        var versions = jdbc.queryForList("SELECT current_published_version_id FROM content_packages "
                + "WHERE code IN ('COURSE55-PRACTICE-1','COURSE65-PRACTICE-1') ORDER BY code", UUID.class);
        assertThat(versions).hasSize(2);
        for (UUID version : versions) {
            var projection = reader.publishedPackageVersion(version).orElseThrow();
            assertThat(projection.packageType().name()).isEqualTo("PRACTICE_TEST");
            assertThat(projection.sections()).hasSize(2);
            var questions = projection.sections().stream().flatMap(s -> s.items().stream()).toList();
            assertThat(questions).hasSize(4);
            assertThat(reader.questionVersionsReservedForLearning(questions.stream()
                    .map(com.ieltspath.content.application.result.PackageVersionContentResult.Item::questionVersionId).toList()))
                    .hasSize(4);
        }
    }

    @Test
    void replacesAndClearsOnlyTheSelectedCourseAndReturnsSortedPackageMetadata() {
        var old = practiceTest("OLD");
        replace(List.of(entry(old, 1)));
        var otherCourseRows = jdbc.queryForList("SELECT * FROM course_practice_test_milestones WHERE course_id<>?",
                course.getId());
        var first = practiceTest("FIRST_TEST");
        var second = practiceTest("SECOND_TEST");

        assertThat(replace(List.of(entry(second, 2), entry(first, 1))))
                .containsExactly(projected(first, 1), projected(second, 2));
        assertThat(get.execute(course.getId(), false))
                .containsExactly(projected(first, 1), projected(second, 2));
        assertThat(replace(List.of())).isEmpty();
        assertThat(get.execute(course.getId(), false)).isEmpty();
        assertThat(get.execute(course.getId(), true)).isEmpty();
        assertThat(jdbc.queryForList("SELECT * FROM course_practice_test_milestones WHERE course_id<>?", course.getId()))
                .containsExactlyInAnyOrderElementsOf(otherCourseRows);
    }

    @ParameterizedTest
    @ValueSource(strings = {"WRONG_TYPE", "DRAFT_PACKAGE", "ARCHIVED_PACKAGE", "DRAFT_VERSION",
            "ARCHIVED_VERSION", "NO_CURRENT_VERSION", "FOREIGN_CURRENT_VERSION", "UNKNOWN_PACKAGE"})
    void rejectsIneligiblePackagesWithoutOverwritingExistingMilestones(String scenario) {
        var candidate = packageFixture("INVALID", scenario.equals("WRONG_TYPE") ? "PRACTICE_SET" : "PRACTICE_TEST",
                scenario.equals("DRAFT_PACKAGE") ? "DRAFT" : scenario.equals("ARCHIVED_PACKAGE") ? "ARCHIVED" : "PUBLISHED",
                scenario.equals("DRAFT_VERSION") ? "DRAFT" : scenario.equals("ARCHIVED_VERSION") ? "ARCHIVED" : "PUBLISHED");
        if (scenario.equals("NO_CURRENT_VERSION")) {
            jdbc.update("UPDATE content_packages SET current_published_version_id=NULL WHERE id=?", candidate.packageId());
        } else if (scenario.equals("FOREIGN_CURRENT_VERSION")) {
            var other = practiceTest("OTHER");
            jdbc.update("UPDATE content_packages SET current_published_version_id=? WHERE id=?",
                    other.versionId(), candidate.packageId());
        }
        UUID packageId = scenario.equals("UNKNOWN_PACKAGE") ? UUID.randomUUID() : candidate.packageId();
        assertRejected(List.of(entry(practiceTest("VALID"), 1), new PracticeTestMilestone(packageId, null, null, null, 2)));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, Integer.MIN_VALUE})
    void rejectsNonpositivePositionsWithoutOverwritingExistingMilestones(int position) {
        assertRejected(List.of(entry(practiceTest("INVALID_POSITION"), position)));
    }

    @Test
    void rejectsDuplicatePackagesEvenAtDifferentPositionsWithoutOverwritingExistingMilestones() {
        var duplicate = practiceTest("DUPLICATE");
        assertRejected(List.of(entry(duplicate, 1), entry(duplicate, 2)));
    }

    @Test
    void rejectsPackagesAssignedToAnotherCourseWithoutChangingEitherCoursesMilestones() {
        UUID otherCourse = UUID.randomUUID();
        jdbc.update("INSERT INTO courses(id,code,name,band_level,status) VALUES (?,'MILESTONE_OTHER','Other',8.0,'ACTIVE')",
                otherCourse);
        var shared = practiceTest("OTHER_COURSE");
        repository.replace(otherCourse, List.of(entry(shared, 1)));
        var originalOtherCourse = repository.findByCourse(otherCourse, false);
        assertRejected(List.of(entry(practiceTest("VALID"), 1), entry(shared, 2)));
        assertThat(repository.findByCourse(otherCourse, false)).containsExactlyElementsOf(originalOtherCourse);
    }

    @Test
    void rejectsUnreachablePositionsAndCountsOnlyDistinctActiveTopicsWithPublishedLessons() {
        UUID firstTopic = jdbc.queryForObject("SELECT id FROM topics WHERE code='MILESTONE_FIRST'", UUID.class);
        jdbc.update("INSERT INTO lessons(id,topic_id,code,title,sort_order,status) VALUES (?,?,'MILESTONE_EXTRA','Extra',2,'PUBLISHED')",
                UUID.randomUUID(), firstTopic);
        assertThat(repository.topicCount(course.getId())).isEqualTo(2);
        assertRejected(List.of(entry(practiceTest("UNREACHABLE"), 3)));
    }

    @Test
    void rejectsMissingEntriesNullIdsAndOversizedListsWithoutOverwritingExistingMilestones() {
        var valid = practiceTest("VALID");
        var old = practiceTest("OLD");
        var original = replace(List.of(entry(old, 1)));
        List<List<PracticeTestMilestone>> invalid = Arrays.asList(null, Arrays.asList((PracticeTestMilestone) null),
                List.of(new PracticeTestMilestone(null, null, null, null, 1)), Collections.nCopies(101, entry(valid, 1)));
        for (var entries : invalid) {
            assertThatThrownBy(() -> replace(entries)).isInstanceOf(InvalidContentStateException.class);
            assertThat(get.execute(course.getId(), false)).containsExactlyElementsOf(original);
        }
    }

    @Test
    void rollsBackTheDeleteAndPartialBatchWhenAReplacementInsertFails() {
        var old = practiceTest("OLD");
        var original = replace(List.of(entry(old, 1)));
        var valid = practiceTest("VALID");
        assertThatThrownBy(() -> transaction.executeWithoutResult(status -> repository.replace(course.getId(), List.of(
                entry(valid, 1), new PracticeTestMilestone(UUID.randomUUID(), null, null, null, 2)))))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(get.execute(course.getId(), false)).containsExactlyElementsOf(original);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void replacementAndClearParticipateInTheCallerTransaction(boolean clear) {
        var old = practiceTest("OLD");
        var original = replace(List.of(entry(old, 1)));
        var replacement = practiceTest("REPLACEMENT");
        assertThatThrownBy(() -> transaction.executeWithoutResult(status -> {
            replace.execute(course.getId(), clear ? List.of() : List.of(entry(replacement, 2)));
            throw new IllegalStateException("Roll back the caller transaction");
        })).isInstanceOf(IllegalStateException.class).hasMessage("Roll back the caller transaction");
        assertThat(get.execute(course.getId(), false)).containsExactlyElementsOf(original);
    }

    @Test
    void projectsTheCurrentPublishedVersionRatherThanTheConfiguredOrNewestVersion() {
        var test = practiceTest("VERSIONED");
        replace(List.of(entry(test, 1)));
        UUID published = version(test.packageId(), 2, "PUBLISHED");
        version(test.packageId(), 3, "DRAFT");
        jdbc.update("UPDATE content_packages SET current_published_version_id=? WHERE id=?", published, test.packageId());
        var expected = new PracticeTestMilestone(test.packageId(), published, test.code(), test.code(), 1);
        assertThat(get.execute(course.getId(), false)).containsExactly(expected);
        assertThat(get.execute(course.getId(), true)).containsExactly(expected);
    }

    @ParameterizedTest
    @ValueSource(strings = {"DRAFT_PACKAGE", "ARCHIVED_PACKAGE", "DRAFT_VERSION", "ARCHIVED_VERSION", "NO_CURRENT_VERSION", "FOREIGN_CURRENT_VERSION", "WRONG_TYPE"})
    void internalReadsHideMilestonesThatAreNoLongerPublishedPracticeTests(String scenario) {
        var valid = practiceTest("VALID");
        var changed = practiceTest("CHANGED");
        replace(List.of(entry(valid, 1), entry(changed, 2)));
        switch (scenario) {
            case "DRAFT_PACKAGE" -> jdbc.update("UPDATE content_packages SET status='DRAFT' WHERE id=?", changed.packageId());
            case "ARCHIVED_PACKAGE" -> jdbc.update("UPDATE content_packages SET status='ARCHIVED' WHERE id=?", changed.packageId());
            case "DRAFT_VERSION" -> jdbc.update("UPDATE content_package_versions SET status='DRAFT' WHERE id=?", changed.versionId());
            case "ARCHIVED_VERSION" -> jdbc.update("UPDATE content_package_versions SET status='ARCHIVED' WHERE id=?", changed.versionId());
            case "NO_CURRENT_VERSION" -> jdbc.update("UPDATE content_packages SET current_published_version_id=NULL WHERE id=?", changed.packageId());
            case "FOREIGN_CURRENT_VERSION" -> jdbc.update("UPDATE content_packages SET current_published_version_id=? WHERE id=?",
                    valid.versionId(), changed.packageId());
            case "WRONG_TYPE" -> jdbc.update("UPDATE content_packages SET package_type='PRACTICE_SET' WHERE id=?", changed.packageId());
            default -> throw new IllegalArgumentException(scenario);
        }
        assertThat(get.execute(course.getId(), false)).extracting(PracticeTestMilestone::packageId)
                .containsExactly(valid.packageId(), changed.packageId());
        assertThat(get.execute(course.getId(), true)).containsExactly(projected(valid, 1));
    }

    @Test
    void inactiveCoursesAreHiddenFromInternalReadsButRemainAvailableToAuthors() {
        var test = practiceTest("INACTIVE_COURSE");
        var original = replace(List.of(entry(test, 1)));
        course.update(course.getName(), course.getBandLevel(), ContentStatus.INACTIVE);
        jdbc.update("UPDATE courses SET status='INACTIVE' WHERE id=?", course.getId());
        assertThatThrownBy(() -> get.execute(course.getId(), true)).isInstanceOf(CourseNotFoundException.class);
        assertThat(get.execute(course.getId(), false)).containsExactlyElementsOf(original);
    }

    @Test
    void unknownCoursesAreRejectedByBothReadModesAndReplacement() {
        UUID unknown = UUID.randomUUID();
        assertThatThrownBy(() -> get.execute(unknown, true)).isInstanceOf(CourseNotFoundException.class);
        assertThatThrownBy(() -> get.execute(unknown, false)).isInstanceOf(CourseNotFoundException.class);
        assertThatThrownBy(() -> transaction.execute(status -> replace.execute(unknown, List.of())))
                .isInstanceOf(CourseNotFoundException.class);
    }

    private List<PracticeTestMilestone> replace(List<PracticeTestMilestone> entries) {
        return transaction.execute(status -> replace.execute(course.getId(), entries));
    }

    private void assertRejected(List<PracticeTestMilestone> entries) {
        var old = practiceTest("OLD");
        var original = replace(List.of(entry(old, 1)));
        assertThatThrownBy(() -> replace(entries)).isInstanceOf(InvalidContentStateException.class);
        assertThat(get.execute(course.getId(), false)).containsExactlyElementsOf(original);
    }

    private void topic(String code, int order, String status, String lessonStatus) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO topics(id,code,name,sort_order,skill,course_id,status) VALUES (?,?,?,?,'READING',?,?)",
                id, "MILESTONE_" + code, code, order, course.getId(), status);
        if (lessonStatus != null) {
            jdbc.update("INSERT INTO lessons(id,topic_id,code,title,sort_order,status) VALUES (?,?,?,?,1,?)",
                    UUID.randomUUID(), id, "MILESTONE_LESSON_" + code, code, lessonStatus);
        }
    }

    private PackageFixture practiceTest(String code) {
        return packageFixture(code, "PRACTICE_TEST", "PUBLISHED", "PUBLISHED");
    }

    private PackageFixture packageFixture(String code, String type, String status, String versionStatus) {
        UUID id = UUID.randomUUID();
        String packageCode = "MILESTONE_" + code;
        jdbc.update("INSERT INTO content_packages(id,code,title,package_type,status) VALUES (?,?,?,?,?)",
                id, packageCode, packageCode, type, status);
        UUID version = version(id, 1, versionStatus);
        jdbc.update("UPDATE content_packages SET current_published_version_id=? WHERE id=?", version, id);
        return new PackageFixture(id, version, packageCode);
    }

    private UUID version(UUID packageId, int number, String status) {
        UUID version = UUID.randomUUID();
        jdbc.update("INSERT INTO content_package_versions(id,package_id,version_number,status) VALUES (?,?,?,?)",
                version, packageId, number, status);
        return version;
    }

    private PracticeTestMilestone entry(PackageFixture fixture, int position) {
        return new PracticeTestMilestone(fixture.packageId(), null, null, null, position);
    }

    private PracticeTestMilestone projected(PackageFixture fixture, int position) {
        return new PracticeTestMilestone(fixture.packageId(), fixture.versionId(), fixture.code(), fixture.code(), position);
    }

    private record PackageFixture(UUID packageId, UUID versionId, String code) {}
}
