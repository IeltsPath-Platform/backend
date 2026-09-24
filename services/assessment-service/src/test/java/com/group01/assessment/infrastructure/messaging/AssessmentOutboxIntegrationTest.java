package com.group01.assessment.infrastructure.messaging;

import com.group01.assessment.application.command.FinalizeAssessmentResultCommand;
import com.group01.assessment.application.usecase.FinalizeAssessmentResultUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
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
        String queue = "test.assessment-completed." + UUID.randomUUID();
        amqpAdmin.declareQueue(new Queue(queue, false, false, true));
        Binding binding = BindingBuilder.bind(new Queue(queue)).to(assessmentEventsExchange).with("assessment.completed.v2");
        amqpAdmin.declareBinding(binding);
        UUID eventId = UUID.randomUUID();

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
    }

    private UUID seedGradedDraftResult() {
        UUID attemptId = UUID.randomUUID();
        UUID sectionId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        UUID resultId = UUID.randomUUID();
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
                """, itemId, UUID.randomUUID());
        jdbc.update("""
                INSERT INTO assessment_results (id, attempt_id, result_version, status) VALUES (?, ?, 1, 'DRAFT')
                """, resultId, attemptId);
        jdbc.update("""
                INSERT INTO item_results (id, result_id, attempt_item_id, score, max_score, is_correct, feedback_snapshot)
                VALUES (?, ?, ?, 1.00, 1.00, TRUE, '{}'::jsonb)
                """, UUID.randomUUID(), resultId, itemId);
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
