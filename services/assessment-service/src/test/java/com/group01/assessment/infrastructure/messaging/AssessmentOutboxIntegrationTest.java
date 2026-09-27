package com.group01.assessment.infrastructure.messaging;

import com.group01.assessment.application.command.FinalizeAssessmentResultCommand;
import com.group01.assessment.application.command.ItemResultInput;
import com.group01.assessment.application.command.KnowledgeJudgmentInput;
import com.group01.assessment.application.command.SaveGradingDetailsCommand;
import com.group01.assessment.application.result.AssessmentResultResult;
import com.group01.assessment.application.usecase.CreateAssessmentResultUseCase;
import com.group01.assessment.application.usecase.FinalizeAssessmentResultUseCase;
import com.group01.assessment.application.usecase.SaveAssessmentResultDetailsUseCase;
import com.group01.assessment.domain.exception.InvalidAssessmentStateException;
import com.group01.assessment.domain.vo.QualitativeJudgment;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Verifies the outbox guarantees against real PostgreSQL and RabbitMQ rather than mocks. */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false",
        "app.auth.internal-jwt-issuer=urn:test:gateway",
        "app.auth.internal-jwt-secret=dGVzdC1vbmx5LWludGVybmFsLWp3dC1zaWduaW5nLWtleQ==",
        "assessment.outbox.relay.enabled=false"
})
class AssessmentOutboxIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    @Container
    static final GenericContainer<?> RABBITMQ = new GenericContainer<>("rabbitmq:3.13-alpine")
            .withExposedPorts(5672)
            .waitingFor(Wait.forLogMessage(".*Server startup complete.*", 1));

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.rabbitmq.host", RABBITMQ::getHost);
        registry.add("spring.rabbitmq.port", () -> RABBITMQ.getMappedPort(5672));
        registry.add("spring.rabbitmq.username", () -> "guest");
        registry.add("spring.rabbitmq.password", () -> "guest");
        registry.add("spring.rabbitmq.publisher-confirm-type", () -> "correlated");
        registry.add("spring.rabbitmq.publisher-returns", () -> "true");
        registry.add("spring.rabbitmq.template.mandatory", () -> "true");
    }

    @Autowired FinalizeAssessmentResultUseCase finalizeResult;
    @Autowired CreateAssessmentResultUseCase createResult;
    @Autowired SaveAssessmentResultDetailsUseCase saveDetails;
    @Autowired OutboxRelay relay;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired AmqpAdmin amqpAdmin;
    @Autowired RabbitTemplate rabbitTemplate;
    @Autowired TopicExchange assessmentEventsExchange;

    @Test
    void finalizedResultAndItsOutboxEventCommitTogether() {
        UUID resultId = seedGradedDraftResult();

        finalizeResult.execute(new FinalizeAssessmentResultCommand(resultId));

        assertEquals("COMPLETED", resultStatus(resultId));
        assertEquals(1, outboxRows(resultId));
    }

    @Test
    void rolledBackFinalizationLeavesNeitherCompletedResultNorEvent() {
        UUID resultId = seedGradedDraftResult();

        assertThrows(IllegalStateException.class, () -> new TransactionTemplate(transactionManager).executeWithoutResult(
                status -> {
                    finalizeResult.execute(new FinalizeAssessmentResultCommand(resultId));
                    throw new IllegalStateException("failure after finalization inside the same transaction");
                }));

        assertEquals("DRAFT", resultStatus(resultId));
        assertEquals(0, outboxRows(resultId));
    }

    @Test
    void relayPublishesOnlyCommittedOutboxRows() throws Exception {
        String queue = declareTestQueue();
        UUID eventId = UUID.randomUUID();
        try {
            try (Connection uncommitted = DriverManager.getConnection(
                    POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())) {
                uncommitted.setAutoCommit(false);
                try (PreparedStatement insert = uncommitted.prepareStatement("""
                        INSERT INTO outbox_events (id, aggregate_type, aggregate_id, event_type, payload)
                        VALUES (?, 'AssessmentResult', ?, 'AssessmentCompleted.v2', '{"event_id":"test"}'::jsonb)
                        """)) {
                    insert.setObject(1, eventId);
                    insert.setString(2, UUID.randomUUID().toString());
                    insert.executeUpdate();
                }

                relay.publishPendingBatch();
                assertNull(rabbitTemplate.receive(queue, 500), "an uncommitted outbox row must not be published");

                uncommitted.commit();
            }

            relay.publishPendingBatch();
            Message message = rabbitTemplate.receive(queue, 5000);
            assertNotNull(message);
            assertEquals(eventId.toString(), message.getMessageProperties().getMessageId());
            assertEquals("AssessmentCompleted.v2", message.getMessageProperties().getType());
            assertTrue(new String(message.getBody(), StandardCharsets.UTF_8).contains("event_id"));
            assertNotNull(jdbc.queryForObject("SELECT published_at FROM outbox_events WHERE id = ?",
                    java.sql.Timestamp.class, eventId));
        } finally {
            amqpAdmin.deleteQueue(queue);
        }
    }

    @Test
    void graderLifecycleFinalizesWithTheGradersBandAndPublishesOneEvent() throws Exception {
        String queue = declareTestQueue();
        try {
            UUID knowledgePointId = UUID.randomUUID();
            SubmittedAttempt attempt = seedSubmittedAttempt(knowledgePointId);
            // The learner opened the version with a self-declared band; the grader's band must replace it.
            UUID resultId = jdbc.queryForObject("SELECT gen_random_uuid()", UUID.class);
            jdbc.update("""
                    INSERT INTO assessment_results (id, attempt_id, result_version, status, overall_band)
                    VALUES (?, ?, 1, 'DRAFT', 9.0)
                    """, resultId, attempt.attemptId());
            assertThrows(InvalidAssessmentStateException.class,
                    () -> createResult.executeForGrader(attempt.attemptId(), null),
                    "the learner's draft is still being graded");

            saveDetails.executeForGrader(new SaveGradingDetailsCommand(resultId, 6.5, null,
                    List.of(new ItemResultInput(attempt.itemId(), 1.0, 1.0, true, null, "{}")), null,
                    List.of(new KnowledgeJudgmentInput(attempt.itemId(), knowledgePointId, QualitativeJudgment.PASS))));
            AssessmentResultResult completed = finalizeResult.execute(new FinalizeAssessmentResultCommand(resultId));
            finalizeResult.execute(new FinalizeAssessmentResultCommand(resultId));

            assertEquals("COMPLETED", completed.status());
            assertEquals(6.5, completed.overallBand());
            assertEquals(1, outboxRows(resultId));

            relay.publishPendingBatch();
            List<String> published = eventsFor(queue, resultId);
            assertEquals(1, published.size(), "a repeated finalize must not emit a second event");
            assertTrue(published.get(0).contains("\"PASS\""));
            assertEquals(6.5, new ObjectMapper().readTree(published.get(0)).at("/data/overall_band").asDouble(),
                    "the event carries the grader's band");

            // A regrade opens version 2 and announces it on its own.
            AssessmentResultResult regrade = createResult.executeForGrader(attempt.attemptId(), null);
            assertEquals(2, regrade.resultVersion());
            saveDetails.executeForGrader(new SaveGradingDetailsCommand(regrade.id(), 5.0, null,
                    List.of(new ItemResultInput(attempt.itemId(), 0.0, 1.0, false, null, "{}")), null, null));
            finalizeResult.execute(new FinalizeAssessmentResultCommand(regrade.id()));
            relay.publishPendingBatch();
            assertEquals(1, eventsFor(queue, regrade.id()).size());
        } finally {
            amqpAdmin.deleteQueue(queue);
        }
    }

    /** Bodies on the queue that announce {@code resultId}; other tests' outbox rows are published by the same relay. */
    private List<String> eventsFor(String queue, UUID resultId) {
        List<String> bodies = new java.util.ArrayList<>();
        Message message;
        while ((message = rabbitTemplate.receive(queue, 1000)) != null) {
            String body = new String(message.getBody(), StandardCharsets.UTF_8);
            if (body.contains(resultId.toString())) {
                bodies.add(body);
            }
        }
        return bodies;
    }

    /**
     * Not auto-delete: RabbitMQ drops an auto-delete queue as soon as its last consumer cancels, and every
     * {@code receive(queue, timeout)} consumes and cancels, so a second receive would hit a deleted queue.
     */
    private String declareTestQueue() {
        String queue = "test.assessment-completed." + UUID.randomUUID();
        amqpAdmin.declareQueue(new Queue(queue, false, false, false));
        amqpAdmin.declareBinding(BindingBuilder.bind(new Queue(queue)).to(assessmentEventsExchange)
                .with("assessment.completed.v2"));
        return queue;
    }

    private record SubmittedAttempt(UUID attemptId, UUID itemId) {
    }

    private SubmittedAttempt seedSubmittedAttempt(UUID knowledgePointId) {
        UUID attemptId = UUID.randomUUID();
        UUID sectionId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO assessment_attempts (id, user_id, package_version_id, attempt_type, mode, channel, status,
                                                 started_at, submitted_at, learning_goal_id)
                VALUES (?, ?, ?, 'QUIZ', 'STANDARD', 'WEB', 'SUBMITTED', now(), now(), ?)
                """, attemptId, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        jdbc.update("""
                INSERT INTO attempt_sections (id, attempt_id, content_section_id, sort_order, section_snapshot)
                VALUES (?, ?, ?, 0, '{}'::jsonb)
                """, sectionId, attemptId, UUID.randomUUID());
        jdbc.update("""
                INSERT INTO attempt_items (id, attempt_section_id, question_version_id, sort_order, question_snapshot)
                VALUES (?, ?, ?, 0, '{}'::jsonb)
                """, itemId, sectionId, UUID.randomUUID());
        jdbc.update("""
                INSERT INTO attempt_item_knowledge_points (attempt_item_id, knowledge_point_id, weight)
                VALUES (?, ?, 1.00)
                """, itemId, knowledgePointId);
        return new SubmittedAttempt(attemptId, itemId);
    }

    private UUID seedGradedDraftResult() {
        SubmittedAttempt attempt = seedSubmittedAttempt(UUID.randomUUID());
        UUID resultId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO assessment_results (id, attempt_id, result_version, status) VALUES (?, ?, 1, 'DRAFT')
                """, resultId, attempt.attemptId());
        jdbc.update("""
                INSERT INTO item_results (id, result_id, attempt_item_id, score, max_score, is_correct, feedback_snapshot)
                VALUES (?, ?, ?, 1.00, 1.00, TRUE, '{}'::jsonb)
                """, UUID.randomUUID(), resultId, attempt.itemId());
        return resultId;
    }

    private String resultStatus(UUID resultId) {
        return jdbc.queryForObject("SELECT status FROM assessment_results WHERE id = ?", String.class, resultId);
    }

    private int outboxRows(UUID resultId) {
        Integer count = jdbc.queryForObject(
                "SELECT count(*) FROM outbox_events WHERE aggregate_id = ? AND event_type = 'AssessmentCompleted.v2'",
                Integer.class, resultId.toString());
        return count == null ? 0 : count;
    }
}
