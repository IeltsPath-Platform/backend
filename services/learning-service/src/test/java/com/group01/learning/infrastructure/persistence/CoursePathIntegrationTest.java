package com.group01.learning.infrastructure.persistence;

import com.group01.learning.application.command.AssessmentResult;
import com.group01.learning.application.command.AssessmentResult.ItemResult;
import com.group01.learning.application.port.LearningContentClient;
import com.group01.learning.application.result.CourseResult;
import com.group01.learning.application.usecase.ApplyAssessmentResultUseCase;
import com.group01.learning.application.usecase.AssignCourseTestUseCase;
import com.group01.learning.application.usecase.ListCoursesUseCase;
import com.group01.learning.application.usecase.RefreshLearningTopicsUseCase;
import com.group01.learning.domain.vo.LearningSkill;
import com.group01.learning.domain.vo.TopicStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = {
        "spring.config.import=",
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.open-in-view=false",
        "management.health.rabbit.enabled=false",
        "learning.messaging.consumer-enabled=false",
        "app.auth.internal-jwt-issuer=urn:code-base:api-gateway"
})
@Testcontainers(disabledWithoutDocker = true)
class CoursePathIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("learning_db").withPassword(UUID.randomUUID().toString());

    private static final byte[] INTERNAL_KEY = new byte[32];
    static { new SecureRandom().nextBytes(INTERNAL_KEY); }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("app.auth.internal-jwt-secret", () -> Base64.getEncoder().encodeToString(INTERNAL_KEY));
    }

    private static final UUID USER = UUID.randomUUID();
    private static final UUID LOWER = UUID.fromString("30000000-0000-4000-8000-000000000001");
    private static final UUID HIGHER = UUID.fromString("30000000-0000-4000-8000-000000000002");
    private static final UUID R1 = UUID.randomUUID();
    private static final UUID R2 = UUID.randomUUID();
    private static final UUID L1 = UUID.randomUUID();
    private static final UUID R3 = UUID.randomUUID();
    private static final UUID PACKAGE_A = UUID.randomUUID();
    private static final UUID VERSION_A = UUID.randomUUID();
    private static final UUID PACKAGE_B = UUID.randomUUID();
    private static final UUID VERSION_B = UUID.randomUUID();
    private static final Instant COMPLETED = Instant.parse("2026-10-07T00:00:00Z");

    @Autowired JdbcTemplate jdbc;
    @Autowired RefreshLearningTopicsUseCase refresh;
    @Autowired ListCoursesUseCase listCourses;
    @Autowired AssignCourseTestUseCase assignCourseTest;
    @Autowired ApplyAssessmentResultUseCase applyResult;
    @MockitoBean LearningContentClient content;

    @BeforeEach
    void setUp() {
        jdbc.execute("TRUNCATE course_test_assignments, course_progress, learner_placements, topic_progress, "
                + "knowledge_point_catalog, lesson_progress, assessment_result_versions, kp_evidence, review_items "
                + "CASCADE");
        var lower = new LearningContentClient.Course(LOWER, "IELTS_5_5", "IELTS 5.5", new BigDecimal("5.5"), true);
        var higher = new LearningContentClient.Course(HIGHER, "IELTS_6_5", "IELTS 6.5", new BigDecimal("6.5"), true);
        when(content.getTopicSequence()).thenReturn(List.of(
                topic(R1, "R1", 900, LearningSkill.READING, lower),
                topic(R2, "R2", 910, LearningSkill.READING, lower),
                topic(L1, "L1", 920, LearningSkill.LISTENING, lower),
                topic(R3, "R3", 960, LearningSkill.READING, higher)));
        when(content.getCourseTestPackages(LOWER)).thenReturn(List.of(
                new LearningContentClient.TestPackage(PACKAGE_A, VERSION_A, "C5-A"),
                new LearningContentClient.TestPackage(PACKAGE_B, VERSION_B, "C5-B")));
    }

    @Test
    void listsIndependentCoursePathsAndRecommendsTheLowestBandAtOrAbovePlacement() {
        List<CourseResult> first = listCourses.execute(USER);
        assertEquals(List.of("IELTS_5_5", "IELTS_6_5"), first.stream().map(CourseResult::code).toList());
        assertEquals(3, first.getFirst().topicCount());
        assertEquals(0, first.getFirst().passedTopicCount());
        assertEquals("LOCKED", first.getFirst().testStatus());
        assertFalse(first.getFirst().recommended());
        assertEquals(TopicStatus.IN_PROGRESS, refresh.execute(USER).getFirst().status());
        assertEquals(TopicStatus.IN_PROGRESS, refresh.execute(USER).get(3).status());

        jdbc.update("INSERT INTO learner_placements (user_id, band, attempt_id, completed_at, updated_at) "
                + "VALUES (?, 6.0, ?, ?, ?)", USER, UUID.randomUUID(),
                java.sql.Timestamp.from(COMPLETED), java.sql.Timestamp.from(COMPLETED));

        List<CourseResult> recommended = listCourses.execute(USER);
        assertFalse(recommended.get(0).recommended());
        assertTrue(recommended.get(1).recommended());
    }

    @Test
    void locksCourseTestUntilEveryTopicPassesThenConsumesRotatesAndMarksCoursePassed() {
        var locked = assertThrows(com.group01.learning.application.exception.LearningRequestException.class,
                () -> assignCourseTest.execute(USER, LOWER));
        assertEquals("COURSE_TEST_LOCKED", locked.getCode());

        refresh.execute(USER);
        jdbc.update("UPDATE topic_progress SET passed_at = ? WHERE user_id = ? AND course_id = ?",
                java.sql.Timestamp.from(COMPLETED), USER, LOWER);
        var first = assignCourseTest.execute(USER, LOWER);
        assertEquals(PACKAGE_A, first.packageId());
        assertEquals(first, assignCourseTest.execute(USER, LOWER));

        UUID attempt = UUID.randomUUID();
        AssessmentResult passed = courseGate(attempt, VERSION_A, 4);
        applyResult.execute(passed);
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM course_test_assignments "
                + "WHERE consumed_attempt_id = ? AND consumed_at IS NOT NULL", Integer.class, attempt));
        assertNotNull(jdbc.queryForObject("SELECT passed_at FROM course_progress "
                + "WHERE user_id = ? AND course_id = ?", java.sql.Timestamp.class, USER, LOWER));
        applyResult.execute(passed);
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM course_progress WHERE user_id = ? AND course_id = ?",
                Integer.class, USER, LOWER));
        assertEquals("PASSED", listCourses.execute(USER).getFirst().testStatus());

        var alreadyPassed = assertThrows(com.group01.learning.application.exception.LearningRequestException.class,
                () -> assignCourseTest.execute(USER, LOWER));
        assertEquals("COURSE_ALREADY_PASSED", alreadyPassed.getCode());
    }

    @Test
    void aFailedCourseTestConsumesItsCodeAndTheNextAssignmentRotatesToAnotherPackage() {
        refresh.execute(USER);
        jdbc.update("UPDATE topic_progress SET passed_at = ? WHERE user_id = ? AND course_id = ?",
                java.sql.Timestamp.from(COMPLETED), USER, LOWER);
        var first = assignCourseTest.execute(USER, LOWER);
        applyResult.execute(courseGate(UUID.randomUUID(), first.packageVersionId(), 3));

        var next = assignCourseTest.execute(USER, LOWER);
        assertEquals(PACKAGE_B, next.packageId());
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM course_progress WHERE user_id = ? AND course_id = ?",
                Integer.class, USER, LOWER));
    }

    private AssessmentResult courseGate(UUID attemptId, UUID packageVersionId, int correctCount) {
        List<ItemResult> items = java.util.stream.IntStream.range(0, 5).mapToObj(index -> new ItemResult(
                UUID.randomUUID(), UUID.randomUUID(), index < correctCount,
                index < correctCount ? BigDecimal.ONE : BigDecimal.ZERO, BigDecimal.ONE, List.of())).toList();
        return new AssessmentResult(UUID.randomUUID(), USER, packageVersionId, attemptId, UUID.randomUUID(), 1,
                "COURSE_GATE", COMPLETED, items);
    }

    private static LearningContentClient.Topic topic(UUID id, String code, int order, LearningSkill skill,
                                                       LearningContentClient.Course course) {
        return new LearningContentClient.Topic(id, code, code, order, null, List.of(), skill, true, course);
    }
}
