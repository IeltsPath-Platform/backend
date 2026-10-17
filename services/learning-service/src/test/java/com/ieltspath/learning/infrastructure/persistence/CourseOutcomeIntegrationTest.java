package com.ieltspath.learning.infrastructure.persistence;

import com.ieltspath.learning.application.command.AssessmentResult;
import com.ieltspath.learning.application.command.AssessmentResult.ItemResult;
import com.ieltspath.learning.application.command.AssessmentResult.KnowledgePointJudgment;
import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.application.port.LearningContentClient;
import com.ieltspath.learning.application.result.CourseScoreResult;
import com.ieltspath.learning.application.usecase.ApplyAssessmentResultUseCase;
import com.ieltspath.learning.application.usecase.AssignCourseTestUseCase;
import com.ieltspath.learning.application.usecase.GetCourseScoreUseCase;
import com.ieltspath.learning.application.usecase.ManageScoreConfigUseCase;
import com.ieltspath.learning.application.usecase.RefreshLearningTopicsUseCase;
import com.ieltspath.learning.domain.vo.LearningSkill;
import com.ieltspath.learning.domain.vo.TopicStatus;
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
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Mid-course and final scoring: a topic test taken at any score opens the next topic, the course test opens once every
 * topic test is taken, and the course is passed by the weighted total, not by the course test alone.
 */
@SpringBootTest(properties = {
        "spring.config.import=", "spring.cloud.config.enabled=false", "eureka.client.enabled=false",
        "spring.jpa.hibernate.ddl-auto=validate", "spring.jpa.open-in-view=false",
        "management.health.rabbit.enabled=false", "learning.messaging.consumer-enabled=false",
        "app.auth.internal-jwt-issuer=urn:code-base:api-gateway"
})
@Testcontainers(disabledWithoutDocker = true)
class CourseOutcomeIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("learning_db").withPassword(UUID.randomUUID().toString());
    private static final byte[] KEY = new byte[32];
    static { new SecureRandom().nextBytes(KEY); }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("app.auth.internal-jwt-secret", () -> Base64.getEncoder().encodeToString(KEY));
    }

    private static final UUID COURSE = UUID.fromString("40000000-0000-4000-8000-000000000001");
    private static final UUID TOPIC_ONE = UUID.randomUUID();
    private static final UUID TOPIC_TWO = UUID.randomUUID();
    private static final UUID TOPIC_ONE_PACKAGE = UUID.randomUUID();
    private static final UUID TOPIC_ONE_VERSION = UUID.randomUUID();
    private static final UUID TOPIC_TWO_PACKAGE = UUID.randomUUID();
    private static final UUID TOPIC_TWO_VERSION = UUID.randomUUID();
    private static final UUID PACKAGE_A = UUID.randomUUID();
    private static final UUID VERSION_A = UUID.randomUUID();
    private static final UUID PACKAGE_B = UUID.randomUUID();
    private static final UUID VERSION_B = UUID.randomUUID();
    private static final Instant COMPLETED = Instant.parse("2026-10-07T00:00:00Z");

    @Autowired JdbcTemplate jdbc;
    @Autowired RefreshLearningTopicsUseCase refresh;
    @Autowired AssignCourseTestUseCase assignCourseTest;
    @Autowired ApplyAssessmentResultUseCase applyResult;
    @Autowired GetCourseScoreUseCase score;
    @Autowired ManageScoreConfigUseCase config;
    @MockitoBean LearningContentClient content;

    private final UUID user = UUID.randomUUID();
    private final UUID wrongPoint = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        jdbc.update("UPDATE course_score_configs SET process_weight = 0.2, mid_weight = 0.3, final_weight = 0.5, "
                + "practice_share = 0.64, practice_test_share = 0.36, pass_threshold = 0.7, "
                + "mid_remediation_below = 0.6 WHERE course_id IS NULL");
        var course = new LearningContentClient.Course(COURSE, "IELTS_5_5", "IELTS 5.5", new BigDecimal("5.5"), true);
        when(content.getTopicSequence()).thenReturn(List.of(
                new LearningContentClient.Topic(TOPIC_ONE, "T1", "T1", 900, null, List.of(), LearningSkill.READING,
                        true, course),
                new LearningContentClient.Topic(TOPIC_TWO, "T2", "T2", 910, null, List.of(), LearningSkill.READING,
                        true, course)));
        when(content.getCourseTestPackages(COURSE)).thenReturn(List.of(
                new LearningContentClient.TestPackage(PACKAGE_A, VERSION_A, "C-A"),
                new LearningContentClient.TestPackage(PACKAGE_B, VERSION_B, "C-B")));
        when(content.getPracticeTestMilestones(COURSE)).thenReturn(List.of());
    }

    @Test
    void aWeakTopicTestStillOpensTheNextTopicAndTheCourseTestAndTheWeightedTotalDecidesTheCourse() {
        refresh.execute(user);
        assertEquals(TopicStatus.IN_PROGRESS, status(TOPIC_ONE));
        assertEquals(TopicStatus.LOCKED, status(TOPIC_TWO));

        // 50% on the first topic test: the topic is done, the next one opens, and supplementary work is asked for.
        topicTest(TOPIC_ONE, TOPIC_ONE_PACKAGE, TOPIC_ONE_VERSION, 2, 4);
        assertEquals(TopicStatus.COMPLETED, status(TOPIC_ONE));
        assertEquals(TopicStatus.IN_PROGRESS, status(TOPIC_TWO));
        assertEquals(50.0, jdbc.queryForObject("SELECT first_test_percent FROM topic_progress "
                + "WHERE user_id = ? AND topic_id = ?", Double.class, user, TOPIC_ONE));
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM remediation_flags WHERE user_id = ? "
                + "AND kind = 'MID_COURSE' AND topic_id = ?", Integer.class, user, TOPIC_ONE));
        assertEquals("COURSE_TEST_LOCKED", assertThrows(LearningRequestException.class,
                () -> assignCourseTest.execute(user, COURSE)).getCode(), "the second topic test is still to take");

        topicTest(TOPIC_TWO, TOPIC_TWO_PACKAGE, TOPIC_TWO_VERSION, 4, 4);
        assertEquals(TopicStatus.PASSED, status(TOPIC_TWO));
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM remediation_flags WHERE user_id = ?",
                Integer.class, user), "a strong first test asks for nothing");

        // Every topic test is taken, so the course test opens whatever the scores were.
        var first = assignCourseTest.execute(user, COURSE);
        assertEquals(PACKAGE_A, first.packageId());
        UUID review = UUID.randomUUID();
        jdbc.update("INSERT INTO review_items (id, user_id, knowledge_point_id, lesson_id, status, skill, "
                + "trigger_kind) VALUES (?, ?, ?, ?, 'PENDING', 'READING', 'ASSESSMENT')", review, user, wrongPoint,
                UUID.randomUUID());
        courseTest(VERSION_A, 3, 5);

        // mid = (0.5 + 1.0) / 2 = 0.75, final = 0.6, Practice 0: total = 0.3 * 0.75 + 0.5 * 0.6 = 0.525
        CourseScoreResult remediation = score.execute(user, COURSE);
        assertEquals("REMEDIATION", remediation.status());
        assertEquals(0.525, remediation.total(), 1e-9);
        assertEquals(0.75, remediation.components().mid(), 1e-9);
        assertEquals(0.6, remediation.components().finalTest(), 1e-9);
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM course_progress WHERE user_id = ?",
                Integer.class, user));
        assertTrue(remediation.remediation().stream().anyMatch(flag -> flag.kind().equals("POST_COURSE") && flag.open()));
        assertEquals("REMEDIATION_REQUIRED", assertThrows(LearningRequestException.class,
                () -> assignCourseTest.execute(user, COURSE)).getCode());

        // The supplementary work is done: the learner may retest with another code.
        jdbc.update("UPDATE review_items SET status = 'DONE' WHERE id = ?", review);
        assertEquals("RETEST_READY", score.execute(user, COURSE).status());
        assertEquals(PACKAGE_B, assignCourseTest.execute(user, COURSE).packageId());
        courseTest(VERSION_B, 5, 5);

        // total = 0.3 * 0.75 + 0.5 * 1.0 = 0.725
        CourseScoreResult passed = score.execute(user, COURSE);
        assertEquals("PASSED", passed.status());
        assertEquals(0.725, passed.total(), 1e-9);
        assertNotNull(jdbc.queryForObject("SELECT passed_at FROM course_progress WHERE user_id = ? AND course_id = ?",
                Timestamp.class, user, COURSE));
        assertEquals("COURSE_ALREADY_PASSED", assertThrows(LearningRequestException.class,
                () -> assignCourseTest.execute(user, COURSE)).getCode());
    }

    @Test
    void anAdminChangeMovesTheScoreOfAnUndecidedCourseButNotOfAPassedOne() {
        refresh.execute(user);
        topicTest(TOPIC_ONE, TOPIC_ONE_PACKAGE, TOPIC_ONE_VERSION, 4, 4);
        topicTest(TOPIC_TWO, TOPIC_TWO_PACKAGE, TOPIC_TWO_VERSION, 4, 4);
        assignCourseTest.execute(user, COURSE);
        courseTest(VERSION_A, 3, 5);
        // 0.3 * 1.0 + 0.5 * 0.6 = 0.6: not passed.
        assertEquals(0.6, score.execute(user, COURSE).total(), 1e-9);
        assertEquals("RETEST_READY", score.execute(user, COURSE).status());

        // Process counts for nothing and the final test for more: the same results now add up differently.
        config.update(UUID.randomUUID(), null, new BigDecimal("0.0"), new BigDecimal("0.2"), new BigDecimal("0.8"),
                new BigDecimal("0.64"), new BigDecimal("0.36"), new BigDecimal("0.7"), new BigDecimal("0.6"));
        assertEquals(0.2 * 1.0 + 0.8 * 0.6, score.execute(user, COURSE).total(), 1e-9);

        assignCourseTest.execute(user, COURSE);
        courseTest(VERSION_B, 5, 5);
        assertEquals("PASSED", score.execute(user, COURSE).status());
        double decided = score.execute(user, COURSE).total();

        config.update(UUID.randomUUID(), null, new BigDecimal("0.5"), new BigDecimal("0.5"), new BigDecimal("0.0"),
                new BigDecimal("0.64"), new BigDecimal("0.36"), new BigDecimal("0.7"), new BigDecimal("0.6"));
        assertEquals(decided, score.execute(user, COURSE).total(), 1e-9, "a passed course keeps its decision");
    }

    @Test
    void aCourseCanHaveItsOwnConfigurationAndInvalidOnesAreRefused() {
        var inherited = config.get(COURSE);
        assertTrue(inherited.inherited());
        assertEquals(0, new BigDecimal("0.7").compareTo(inherited.passThreshold()));

        config.update(UUID.randomUUID(), COURSE, new BigDecimal("0.1"), new BigDecimal("0.1"), new BigDecimal("0.8"),
                new BigDecimal("0.5"), new BigDecimal("0.5"), new BigDecimal("0.75"), new BigDecimal("0.5"));
        assertFalse(config.get(COURSE).inherited());
        assertEquals(0, new BigDecimal("0.75").compareTo(config.get(COURSE).passThreshold()));
        assertEquals(0, new BigDecimal("0.7").compareTo(config.get(null).passThreshold()));

        var refused = assertThrows(LearningRequestException.class, () -> config.update(UUID.randomUUID(), COURSE,
                new BigDecimal("0.3"), new BigDecimal("0.3"), new BigDecimal("0.5"), new BigDecimal("0.5"),
                new BigDecimal("0.5"), new BigDecimal("0.7"), new BigDecimal("0.6")));
        assertEquals("INVALID_SCORE_CONFIG", refused.getCode());
        assertEquals(400, refused.getStatus());
    }

    private TopicStatus status(UUID topic) {
        return refresh.execute(user).stream().filter(item -> item.topicId().equals(topic)).findFirst().orElseThrow()
                .status();
    }

    private void topicTest(UUID topic, UUID packageId, UUID versionId, int correct, int total) {
        jdbc.update("INSERT INTO topic_test_assignments (id, user_id, topic_id, package_id, package_version_id, "
                + "assigned_at) VALUES (?, ?, ?, ?, ?, ?)", UUID.randomUUID(), user, topic, packageId, versionId,
                Timestamp.from(COMPLETED.minusSeconds(60)));
        applyResult.execute(result("TOPIC_GATE", versionId, correct, total));
    }

    private void courseTest(UUID versionId, int correct, int total) {
        jdbc.update("UPDATE course_test_assignments SET assigned_at = ? WHERE user_id = ? AND consumed_at IS NULL",
                Timestamp.from(COMPLETED.minusSeconds(60)), user);
        applyResult.execute(result("COURSE_GATE", versionId, correct, total));
    }

    private AssessmentResult result(String type, UUID versionId, int correct, int total) {
        List<ItemResult> items = java.util.stream.IntStream.range(0, total).mapToObj(index -> new ItemResult(
                UUID.randomUUID(), UUID.randomUUID(), index < correct,
                index < correct ? BigDecimal.ONE : BigDecimal.ZERO, BigDecimal.ONE,
                List.of(new KnowledgePointJudgment(wrongPoint, null)))).toList();
        return new AssessmentResult(UUID.randomUUID(), user, versionId, UUID.randomUUID(), UUID.randomUUID(), 1, type,
                COMPLETED, items);
    }
}
