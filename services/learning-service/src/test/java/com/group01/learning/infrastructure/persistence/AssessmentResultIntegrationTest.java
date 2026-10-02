package com.group01.learning.infrastructure.persistence;

import com.group01.learning.application.command.AssessmentResult;
import com.group01.learning.application.command.AssessmentResult.ItemResult;
import com.group01.learning.application.command.AssessmentResult.KnowledgePointJudgment;
import com.group01.learning.application.port.LearningContentClient;
import com.group01.learning.application.usecase.ApplyAssessmentResultUseCase;
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
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

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
class AssessmentResultIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("learning_db")
            .withPassword(UUID.randomUUID().toString());

    private static final byte[] INTERNAL_KEY = new byte[32];
    static {
        new SecureRandom().nextBytes(INTERNAL_KEY);
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("app.auth.internal-jwt-secret", () -> Base64.getEncoder().encodeToString(INTERNAL_KEY));
    }

    private static final UUID USER = UUID.randomUUID();
    private static final UUID TOPIC = UUID.randomUUID();
    private static final UUID NEXT_TOPIC = UUID.randomUUID();
    private static final UUID LESSON = UUID.randomUUID();
    private static final UUID KP_RIGHT = UUID.randomUUID();
    private static final UUID KP_WRONG = UUID.randomUUID();
    private static final UUID VERSION = UUID.randomUUID();
    private static final Instant COMPLETED = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    @Autowired JdbcTemplate jdbc;
    @Autowired ApplyAssessmentResultUseCase apply;
    @MockitoBean LearningContentClient content;

    private UUID assignment;

    @BeforeEach
    void seed() {
        jdbc.execute("""
                TRUNCATE review_sets, review_items, lesson_exercise_submissions, kp_evidence,
                lesson_progress, topic_progress, knowledge_point_catalog, topic_test_assignments,
                assessment_result_versions RESTART IDENTITY
                """);
        jdbc.update("INSERT INTO topic_progress (user_id, topic_id, sequence_order) VALUES (?, ?, 1), (?, ?, 2)",
                USER, TOPIC, USER, NEXT_TOPIC);
        jdbc.update("INSERT INTO knowledge_point_catalog (kp_id, topic_id, has_practice_set) VALUES (?, ?, TRUE), (?, ?, TRUE)",
                KP_RIGHT, TOPIC, KP_WRONG, TOPIC);
        jdbc.update("""
                INSERT INTO lesson_progress (user_id, lesson_id, topic_id, lesson_sort_order, knowledge_point_ids, completed_at)
                VALUES (?, ?, ?, 1, ARRAY[?, ?]::uuid[], now())
                """, USER, LESSON, TOPIC, KP_RIGHT, KP_WRONG);
        assignment = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO topic_test_assignments (id, user_id, topic_id, package_id, package_version_id, assigned_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """, assignment, USER, TOPIC, UUID.randomUUID(), VERSION, Timestamp.from(COMPLETED.minusSeconds(600)));
    }

    /** {@code rightItems} correct items on KP_RIGHT plus one wrong item on KP_WRONG. */
    private static AssessmentResult result(String type, UUID attempt, int version, int rightItems, UUID packageVersion) {
        List<ItemResult> items = new java.util.ArrayList<>();
        for (int i = 0; i < rightItems; i++) {
            items.add(item(true, KP_RIGHT));
        }
        items.add(item(false, KP_WRONG));
        return new AssessmentResult(UUID.randomUUID(), USER, packageVersion, attempt, UUID.randomUUID(), version, type,
                COMPLETED, items);
    }

    private static ItemResult item(boolean correct, UUID kp) {
        return new ItemResult(UUID.randomUUID(), UUID.randomUUID(), correct, correct ? BigDecimal.ONE : BigDecimal.ZERO,
                BigDecimal.ONE, List.of(new KnowledgePointJudgment(kp, null)));
    }

    private int assessmentEvidence() {
        return jdbc.queryForObject("SELECT count(*) FROM kp_evidence WHERE source = 'assessment'", Integer.class);
    }

    @Test
    void passingTopicGatePassesTopicConsumesAssignmentAndInsertsReviewForTheMissedKp() {
        AssessmentResult passed = result("TOPIC_GATE", UUID.randomUUID(), 1, 3, VERSION);
        apply.execute(passed);

        assertNotNull(jdbc.queryForObject("SELECT passed_at FROM topic_progress WHERE user_id = ? AND topic_id = ?",
                Timestamp.class, USER, TOPIC));
        assertEquals(75.0, jdbc.queryForObject("SELECT percent FROM topic_test_assignments WHERE id = ?",
                Double.class, assignment));
        assertEquals(KP_WRONG, jdbc.queryForObject(
                "SELECT knowledge_point_id FROM review_items WHERE user_id = ? AND status = 'PENDING'", UUID.class, USER));
        assertEquals(4, assessmentEvidence());

        // Redelivery of the same version changes nothing.
        apply.execute(passed);
        assertEquals(4, assessmentEvidence());
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM review_items", Integer.class));
    }

    @Test
    void failingTopicGateConsumesTheAssignmentButKeepsTheTopicOpen() {
        apply.execute(result("TOPIC_GATE", UUID.randomUUID(), 1, 1, VERSION));
        assertNull(jdbc.queryForObject("SELECT passed_at FROM topic_progress WHERE user_id = ? AND topic_id = ?",
                Timestamp.class, USER, TOPIC));
        assertEquals(50.0, jdbc.queryForObject("SELECT percent FROM topic_test_assignments WHERE id = ?",
                Double.class, assignment));
    }

    @Test
    void regradeReplacesTheAttemptEvidenceAndNeverRevokesThePass() {
        UUID attempt = UUID.randomUUID();
        apply.execute(result("TOPIC_GATE", attempt, 1, 3, VERSION));
        apply.execute(result("TOPIC_GATE", attempt, 2, 1, VERSION));

        assertEquals(2, jdbc.queryForObject(
                "SELECT count(*) FROM kp_evidence WHERE attempt_id = ? AND result_version = 2", Integer.class, attempt));
        assertEquals(0, jdbc.queryForObject(
                "SELECT count(*) FROM kp_evidence WHERE attempt_id = ? AND result_version = 1", Integer.class, attempt));
        assertNotNull(jdbc.queryForObject("SELECT passed_at FROM topic_progress WHERE user_id = ? AND topic_id = ?",
                Timestamp.class, USER, TOPIC));

        // A stale version arriving late is ignored.
        apply.execute(result("TOPIC_GATE", attempt, 1, 3, VERSION));
        assertEquals(2, jdbc.queryForObject("SELECT result_version FROM assessment_result_versions WHERE attempt_id = ?",
                Integer.class, attempt));
        assertEquals(2, assessmentEvidence());
    }

    @Test
    void placementRecordsOnlyTheVersionAndAnAttemptBeforeTheAssignmentDoesNotOpenTheTopic() {
        apply.execute(result("PLACEMENT", UUID.randomUUID(), 1, 3, VERSION));
        assertEquals(0, assessmentEvidence());
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM assessment_result_versions", Integer.class));

        jdbc.update("UPDATE topic_test_assignments SET assigned_at = ? WHERE id = ?",
                Timestamp.from(COMPLETED.plusSeconds(60)), assignment);
        apply.execute(result("TOPIC_GATE", UUID.randomUUID(), 1, 3, VERSION));
        assertNull(jdbc.queryForObject("SELECT consumed_at FROM topic_test_assignments WHERE id = ?",
                Timestamp.class, assignment));
        assertEquals(4, assessmentEvidence());
    }

    @Test
    void mockResultWithoutPackageVersionStillAddsEvidenceAndReviews() {
        apply.execute(result("MOCK", UUID.randomUUID(), 1, 1, null));
        assertEquals(2, assessmentEvidence());
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM review_items WHERE knowledge_point_id = ?",
                Integer.class, KP_WRONG));
        assertNull(jdbc.queryForObject("SELECT consumed_at FROM topic_test_assignments WHERE id = ?",
                Timestamp.class, assignment));
    }
}
