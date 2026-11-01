package com.ieltspath.notification.infrastructure;

import com.ieltspath.events.access.AccessMessaging;
import com.ieltspath.events.access.SubscriptionGrantedTopology;
import com.ieltspath.events.messaging.DeadLetterReplayer;
import com.ieltspath.events.messaging.ReplayResult;
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
import java.util.UUID;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Replays a message from a dead-letter queue against a real RabbitMQ. The message is malformed, so the listener refuses it
 * again: the replay moves it back once, and it lands in the dead-letter queue again, never lost and never stored.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {
        "spring.cloud.config.enabled=false", "spring.config.import=", "eureka.client.enabled=false",
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
    @Autowired JdbcTemplate jdbc;
    @Autowired DeadLetterReplayer replayer;

    @BeforeEach
    void clean() {
        jdbc.update("DELETE FROM notifications");
        while (rabbit.receive(SubscriptionGrantedTopology.DLQ, 200) != null) {
            // Messages left over from another test are discarded.
        }
    }

    private static boolean eventually(BooleanSupplier condition) throws InterruptedException {
        for (int i = 0; i < 100; i++) {
            if (condition.getAsBoolean()) return true;
            Thread.sleep(100);
        }
        return false;
    }

    @Test
    void aReplayedMessageIsMovedBackOnceAndNeverStored() throws Exception {
        String body = "not json " + UUID.randomUUID();
        Message message = MessageBuilder.withBody(body.getBytes(StandardCharsets.UTF_8))
                .setContentType(MessageProperties.CONTENT_TYPE_JSON).setMessageId(UUID.randomUUID().toString()).build();
        rabbit.send(AccessMessaging.EXCHANGE, AccessMessaging.SUBSCRIPTION_GRANTED_ROUTING_KEY, message);
        assertTrue(eventually(() -> replayer.counts().stream()
                .anyMatch(c -> c.deadLetterQueue().equals(SubscriptionGrantedTopology.DLQ) && c.messages() == 1)),
                "the malformed message reached the dead-letter queue");

        ReplayResult result = replayer.replay(SubscriptionGrantedTopology.DLQ, 10);

        assertEquals(1, result.moved());
        assertTrue(eventually(() -> replayer.counts().stream()
                .anyMatch(c -> c.deadLetterQueue().equals(SubscriptionGrantedTopology.DLQ) && c.messages() == 1)),
                "the listener refused it again, so it is back in the dead-letter queue");
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM notifications", Integer.class));
    }

    @Test
    void onlyDeclaredDeadLetterQueuesCanBeReplayed() {
        assertThrows(IllegalArgumentException.class, () -> replayer.replay("some.other.queue", 1));
        assertThrows(IllegalArgumentException.class,
                () -> replayer.replay(SubscriptionGrantedTopology.DLQ, DeadLetterReplayer.MAX_PER_CALL + 1));
    }
}
