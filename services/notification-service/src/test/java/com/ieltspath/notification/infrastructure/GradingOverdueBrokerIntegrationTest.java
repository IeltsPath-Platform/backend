package com.ieltspath.notification.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.events.assessment.AssessmentMessaging;
import com.ieltspath.events.assessment.GradingOverdueV1;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The whole path inside Notification against a real RabbitMQ and PostgreSQL: the application declares its queue, the
 * listener consumes what Assessment publishes, the notification is stored once, and a bad message lands in the
 * dead-letter queue instead of being retried or lost.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {
        "spring.cloud.config.enabled=false", "spring.config.import=", "eureka.client.enabled=false",
        "app.auth.internal-jwt-issuer=urn:test:gateway",
        "app.auth.internal-jwt-secret=dGVzdC1vbmx5LWludGVybmFsLWp3dC1zaWduaW5nLWtleQ=="
})
class GradingOverdueBrokerIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");
    @Container
    static final GenericContainer<?> RABBIT = new GenericContainer<>("rabbitmq:3.13-management-alpine")
            .withExposedPorts(5672).waitingFor(Wait.forLogMessage(".*Server startup complete.*", 1));

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.rabbitmq.host", RABBIT::getHost);
        registry.add("spring.rabbitmq.port", () -> RABBIT.getMappedPort(5672));
    }

    @Autowired RabbitTemplate rabbit;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper json;

    @BeforeEach
    void clean() {
        jdbc.update("DELETE FROM notifications");
    }

    private GradingOverdueV1 event(UUID learner, UUID job) {
        return new GradingOverdueV1(UUID.randomUUID(), GradingOverdueV1.EVENT_TYPE, Instant.now(), learner,
                UUID.randomUUID(), job, "WRITING", Instant.now().minusSeconds(30));
    }

    /** Published the way Assessment's relay does: JSON body, the event id as message id, the event type as routing key. */
    private void publish(GradingOverdueV1 event, byte[] body) {
        Message message = MessageBuilder.withBody(body).setContentType(MessageProperties.CONTENT_TYPE_JSON)
                .setContentEncoding(StandardCharsets.UTF_8.name()).setMessageId(event.eventId().toString()).build();
        rabbit.send(AssessmentMessaging.EXCHANGE, AssessmentMessaging.GRADING_OVERDUE_ROUTING_KEY, message);
    }

    private static boolean eventually(BooleanSupplier condition) throws InterruptedException {
        for (int i = 0; i < 100; i++) {
            if (condition.getAsBoolean()) return true;
            Thread.sleep(100);
        }
        return false;
    }

    private int notificationsOf(UUID learner) {
        return jdbc.queryForObject("SELECT count(*) FROM notifications WHERE recipient_id=?", Integer.class, learner);
    }

    @Test
    void aLateExaminerNoticeFromAssessmentBecomesOneNotificationForTheLearner() throws Exception {
        UUID learner = UUID.randomUUID(), job = UUID.randomUUID();
        var event = event(learner, job);

        publish(event, json.writeValueAsBytes(event));
        assertTrue(eventually(() -> notificationsOf(learner) == 1), "the notification was stored");

        assertEquals(job, jdbc.queryForObject("SELECT grading_job_id FROM notifications WHERE recipient_id=?", UUID.class, learner));
        assertEquals(event.examinerId(), jdbc.queryForObject("SELECT actor_id FROM notifications WHERE recipient_id=?", UUID.class, learner));
        assertEquals(GradingOverdueV1.EVENT_TYPE, jdbc.queryForObject("SELECT event_type FROM notifications WHERE recipient_id=?", String.class, learner));

        // The broker delivers at least once: the same event again adds nothing.
        publish(event, json.writeValueAsBytes(event));
        Thread.sleep(1500);
        assertEquals(1, notificationsOf(learner));
        assertNull(rabbit.receive(GradingOverdueMessagingConfig.DLQ), "a duplicate is not an error");
    }

    @Test
    void aMessageThatCannotBeUsedIsDeadLetteredAndStoresNothing() throws Exception {
        UUID learner = UUID.randomUUID();
        var unreadable = event(learner, UUID.randomUUID());
        publish(unreadable, "not json".getBytes(StandardCharsets.UTF_8));
        Message parked = rabbit.receive(GradingOverdueMessagingConfig.DLQ, 10_000);
        assertNotNull(parked, "an unreadable message goes to the dead-letter queue");

        // A readable event whose message id is not its event id breaks the contract.
        var forged = event(learner, UUID.randomUUID());
        Message wrongId = MessageBuilder.withBody(json.writeValueAsBytes(forged))
                .setContentType(MessageProperties.CONTENT_TYPE_JSON).setMessageId(UUID.randomUUID().toString()).build();
        rabbit.send(AssessmentMessaging.EXCHANGE, AssessmentMessaging.GRADING_OVERDUE_ROUTING_KEY, wrongId);
        assertNotNull(rabbit.receive(GradingOverdueMessagingConfig.DLQ, 10_000), "a contract break is dead-lettered too");

        assertEquals(0, notificationsOf(learner));
    }
}
