package com.ieltspath.access.infrastructure.messaging;

import com.ieltspath.events.points.PointsGrantMessaging;

import com.ieltspath.access.application.result.PointCreditResult;
import com.ieltspath.access.application.usecase.CreditPointsUseCase;
import com.ieltspath.access.domain.vo.LearningRewardReason;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Runs the real consumer against a real RabbitMQ to confirm what unit tests cannot: that the dead-letter and retry queues
 * are declared the way the consumer relies on, that a refused message lands in the dead-letter queue instead of being
 * redelivered, and that the grant queue keeps serving the messages behind it.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {
        "spring.cloud.config.enabled=false",
        "app.auth.internal-jwt-issuer=urn:code-base:api-gateway"
})
class PointsGrantDeadLetterBrokerTest {
    private static final String INTERNAL_SECRET = "ZmVkY2JhOTg3NjU0MzIxMGZlZGNiYTk4NzY1NDMyMTA=";
    private static final Duration WAIT = Duration.ofSeconds(15);

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");
    @Container
    static final GenericContainer<?> RABBIT = new GenericContainer<>("rabbitmq:3.13-alpine")
            .withExposedPorts(5672).waitingFor(Wait.forLogMessage(".*Server startup complete.*", 1))
            .withStartupTimeout(Duration.ofMinutes(2));

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.rabbitmq.host", RABBIT::getHost);
        registry.add("spring.rabbitmq.port", () -> RABBIT.getMappedPort(5672));
        registry.add("app.auth.internal-jwt-secret", () -> INTERNAL_SECRET);
    }

    @Autowired RabbitTemplate rabbit;
    @MockitoBean CreditPointsUseCase credits;

    private void send(UUID grantId) {
        send(grantId, null);
    }

    private void send(UUID grantId, String messageId) {
        String body = "{\"grantId\":\"" + grantId + "\",\"userId\":\"" + UUID.randomUUID()
                + "\",\"amount\":3,\"reason\":\"DAILY_QUEST\",\"referenceId\":\"quest:1\"}";
        rabbit.send(PointsGrantMessaging.EXCHANGE, PointsGrantMessaging.REQUESTED_ROUTING_KEY,
                MessageBuilder.withBody(body.getBytes(StandardCharsets.UTF_8)).setContentType("application/json")
                        .setMessageId(messageId).build());
    }

    private void creditFailsFor(UUID grantId, RuntimeException failure) {
        when(credits.executeLearningReward(eq(grantId), any(), anyInt(), any(LearningRewardReason.class), anyString()))
                .thenThrow(failure);
    }

    @Test
    void aGrantTheDatabaseRefusesIsDeadLetteredNotRedelivered() {
        UUID poison = UUID.randomUUID();
        creditFailsFor(poison, new DataIntegrityViolationException("duplicate key"));

        send(poison);

        Message dead = rabbit.receive(LearningPointsMessagingConfig.GRANT_DLQ, WAIT.toMillis());
        assertNotNull(dead, "the refused message reaches the dead-letter queue");
        assertEquals(true, new String(dead.getBody(), StandardCharsets.UTF_8).contains(poison.toString()));
        assertNull(rabbit.receive(LearningPointsMessagingConfig.GRANT_QUEUE, 500), "and is not put back on the queue");
        verify(credits, timeout(1000).times(1)).executeLearningReward(eq(poison), any(), anyInt(),
                any(LearningRewardReason.class), anyString());
    }

    @Test
    void aTemporaryFailureIsParkedOnTheRetryQueueWithItsCountAndTheQueueKeepsServing() {
        UUID flaky = UUID.randomUUID();
        UUID healthy = UUID.randomUUID();
        creditFailsFor(flaky, new QueryTimeoutException("database busy"));
        when(credits.executeLearningReward(eq(healthy), any(), anyInt(), any(LearningRewardReason.class), anyString()))
                .thenReturn(new PointCreditResult(3, false, 3, UUID.randomUUID()));

        send(flaky, "message-" + flaky);
        send(healthy);

        Message parked = rabbit.receive(LearningPointsMessagingConfig.GRANT_RETRY_QUEUE, WAIT.toMillis());
        assertNotNull(parked, "the failing message waits on the retry queue");
        assertEquals(1, ((Number) parked.getMessageProperties().getHeader(MessageRetry.ATTEMPTS_HEADER)).intValue());
        assertEquals("message-" + flaky, parked.getMessageProperties().getMessageId(), "same message, same grant id");
        assertEquals(true, new String(parked.getBody(), StandardCharsets.UTF_8).contains(flaky.toString()));
        verify(credits, timeout(WAIT.toMillis())).executeLearningReward(eq(healthy), any(), anyInt(),
                any(LearningRewardReason.class), anyString());
        assertNull(rabbit.receive(LearningPointsMessagingConfig.GRANT_DLQ, 500), "nothing is dead-lettered yet");
    }
}
