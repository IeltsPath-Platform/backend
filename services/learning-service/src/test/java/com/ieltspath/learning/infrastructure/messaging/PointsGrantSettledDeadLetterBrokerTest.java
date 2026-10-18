package com.ieltspath.learning.infrastructure.messaging;

import com.ieltspath.learning.application.service.RewardGrantService;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Runs the real consumer of Access's answers against a real RabbitMQ: an unreadable answer lands in the dead-letter queue
 * instead of being redelivered, a failure to record is parked on the retry queue with its count, and the queue keeps
 * serving the answers behind either.
 */
@SpringBootTest(properties = {
        "spring.config.import=", "spring.cloud.config.enabled=false", "eureka.client.enabled=false",
        "spring.jpa.hibernate.ddl-auto=validate", "spring.jpa.open-in-view=false",
        "management.health.rabbit.enabled=false", "learning.messaging.consumer-enabled=true",
        "learning.mastery.backfill-on-startup=false", "learning.league.close-enabled=false",
        "learning.rewards.relay-enabled=false", "app.auth.internal-jwt-issuer=urn:code-base:api-gateway"
})
@Testcontainers(disabledWithoutDocker = true)
class PointsGrantSettledDeadLetterBrokerTest {
    private static final Duration WAIT = Duration.ofSeconds(15);

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("learning_db").withPassword(UUID.randomUUID().toString());
    @Container
    static final GenericContainer<?> rabbit = new GenericContainer<>("rabbitmq:3.13-alpine")
            .withExposedPorts(5672).waitingFor(Wait.forLogMessage(".*Server startup complete.*", 1))
            .withStartupTimeout(Duration.ofMinutes(2));
    private static final byte[] KEY = new byte[32];
    static { new SecureRandom().nextBytes(KEY); }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.rabbitmq.host", rabbit::getHost);
        registry.add("spring.rabbitmq.port", () -> rabbit.getMappedPort(5672));
        registry.add("app.auth.internal-jwt-secret", () -> Base64.getEncoder().encodeToString(KEY));
    }

    @Autowired RabbitTemplate rabbitTemplate;
    @MockitoBean RewardGrantService grants;

    private void send(String body) {
        rabbitTemplate.send(RewardGrantMessagingConfig.EXCHANGE, RewardGrantMessagingConfig.SETTLED_ROUTING_KEY,
                MessageBuilder.withBody(body.getBytes(StandardCharsets.UTF_8)).setContentType("application/json").build());
    }

    private static String answer(UUID grantId) {
        return "{\"grantId\":\"" + grantId + "\",\"status\":\"GRANTED\",\"error\":null}";
    }

    @Test
    void anUnreadableAnswerIsDeadLetteredAndTheQueueKeepsServing() {
        UUID healthy = UUID.randomUUID();

        send("not json");
        send(answer(healthy));

        Message dead = rabbitTemplate.receive(RewardGrantMessagingConfig.SETTLED_DLQ, WAIT.toMillis());
        assertNotNull(dead, "the unreadable answer reaches the dead-letter queue");
        assertEquals("not json", new String(dead.getBody(), StandardCharsets.UTF_8));
        verify(grants, timeout(WAIT.toMillis())).settle(eq(healthy), eq("GRANTED"), org.mockito.ArgumentMatchers.isNull());
        assertNull(rabbitTemplate.receive(RewardGrantMessagingConfig.SETTLED_QUEUE, 500));
    }

    @Test
    void aFailureToRecordIsParkedOnTheRetryQueueWithItsCount() {
        UUID flaky = UUID.randomUUID();
        when(grants.settle(eq(flaky), anyString(), org.mockito.ArgumentMatchers.any()))
                .thenThrow(new QueryTimeoutException("database busy"));

        send(answer(flaky));

        Message parked = rabbitTemplate.receive(RewardGrantMessagingConfig.SETTLED_RETRY_QUEUE, WAIT.toMillis());
        assertNotNull(parked, "the answer waits on the retry queue");
        assertEquals(1, ((Number) parked.getMessageProperties().getHeader(MessageRetry.ATTEMPTS_HEADER)).intValue());
    }
}
