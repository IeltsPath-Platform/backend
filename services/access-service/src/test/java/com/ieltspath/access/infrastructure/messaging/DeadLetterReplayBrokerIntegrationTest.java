package com.ieltspath.access.infrastructure.messaging;

import com.ieltspath.events.messaging.DeadLetterReplayer;
import com.ieltspath.events.messaging.ReplayResult;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Replays a message from the points-grant dead-letter queue through a real RabbitMQ. */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {
        "spring.cloud.config.enabled=false", "spring.config.import=", "eureka.client.enabled=false",
        "access.outbox.relay.enabled=false",
        "spring.rabbitmq.listener.simple.auto-startup=false",
        "app.auth.internal-jwt-issuer=urn:test:gateway",
        "app.auth.internal-jwt-secret=dGVzdC1vbmx5LWludGVybmFsLWp3dC1zaWduaW5nLWtleQ==",
        "spring.rabbitmq.publisher-confirm-type=correlated", "spring.rabbitmq.publisher-returns=true",
        "spring.rabbitmq.template.mandatory=true"
})
class DeadLetterReplayBrokerIntegrationTest {
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
    @Autowired DeadLetterReplayer replayer;

    @Test
    void aMessageParkedOnThePointsGrantDeadLetterQueueIsMovedBack() {
        Message parked = MessageBuilder.withBody(("parked " + UUID.randomUUID()).getBytes(StandardCharsets.UTF_8))
                .setContentType(MessageProperties.CONTENT_TYPE_JSON).setMessageId(UUID.randomUUID().toString())
                .setHeader("traceparent", "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01")
                .setHeader("x-correlation-id", "replay-correlation-42").build();
        // Publishing to the queue name through the default exchange lands the message on the dead-letter queue.
        rabbit.send("", LearningPointsMessagingConfig.GRANT_DLQ, parked);

        ReplayResult result = replayer.replay(LearningPointsMessagingConfig.GRANT_DLQ, 10);

        assertEquals(1, result.moved());
        Message replayed = rabbit.receive(LearningPointsMessagingConfig.GRANT_QUEUE, 5_000);
        org.junit.jupiter.api.Assertions.assertNotNull(replayed);
        assertEquals("00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01",
                replayed.getMessageProperties().getHeader("traceparent"));
        assertEquals("replay-correlation-42", replayed.getMessageProperties().getHeader("x-correlation-id"));
    }

    @Test
    void onlyDeclaredDeadLetterQueuesAndSensibleBatchSizesAreAccepted() {
        assertThrows(IllegalArgumentException.class, () -> replayer.replay("access.some-other.dlq", 1));
        assertThrows(IllegalArgumentException.class,
                () -> replayer.replay(LearningPointsMessagingConfig.GRANT_DLQ, DeadLetterReplayer.MAX_PER_CALL + 1));
    }
}
