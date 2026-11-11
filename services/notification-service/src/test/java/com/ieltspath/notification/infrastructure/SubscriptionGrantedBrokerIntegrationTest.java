package com.ieltspath.notification.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.events.access.AccessMessaging;
import com.ieltspath.events.access.SubscriptionGrantedTopology;
import com.ieltspath.events.access.SubscriptionGrantedV1;
import com.ieltspath.events.messaging.DeadLetterReplayer;
import com.ieltspath.notification.domain.repository.NotificationRepository;
import com.ieltspath.notification.domain.vo.Notification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doThrow;

/**
 * The path inside Notification for "a Premium subscription was granted", against a real RabbitMQ and PostgreSQL: the
 * listener stores one notice per event id, pushes it once, and dead-letters messages that cannot be used.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {
        "spring.cloud.config.enabled=false", "spring.config.import=", "eureka.client.enabled=false",
        "app.auth.internal-jwt-issuer=urn:test:gateway",
        "app.auth.internal-jwt-secret=dGVzdC1vbmx5LWludGVybmFsLWp3dC1zaWduaW5nLWtleQ=="
})
class SubscriptionGrantedBrokerIntegrationTest {
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
    @Autowired DeadLetterReplayer replayer;
    @MockitoBean NoticePushBridge noticePush;
    @MockitoSpyBean NotificationRepository notifications;

    @BeforeEach
    void clean() {
        jdbc.update("DELETE FROM notifications");
        // The queues live as long as the broker container, so a message dead-lettered late by one test must not reach the next.
        for (String queue : new String[] {SubscriptionGrantedTopology.RETRY_QUEUE, SubscriptionGrantedTopology.DLQ}) {
            rabbit.execute(channel -> channel.queuePurge(queue));
        }
    }

    private static SubscriptionGrantedV1 event(UUID learner) {
        return new SubscriptionGrantedV1(UUID.randomUUID(), SubscriptionGrantedV1.EVENT_TYPE, Instant.now(), learner,
                UUID.randomUUID(), "PREMIUM_MONTHLY", Instant.now().plusSeconds(86_400));
    }

    /** Published the way Access's relay does: JSON body, the event id as message id, the event type as routing key. */
    private void publish(SubscriptionGrantedV1 event, byte[] body, String messageId) {
        Message message = MessageBuilder.withBody(body).setContentType(MessageProperties.CONTENT_TYPE_JSON)
                .setContentEncoding(StandardCharsets.UTF_8.name()).setMessageId(messageId).build();
        rabbit.send(AccessMessaging.EXCHANGE, AccessMessaging.SUBSCRIPTION_GRANTED_ROUTING_KEY, message);
    }

    private static boolean eventually(BooleanSupplier condition) throws InterruptedException {
        return eventually(condition, 100);
    }

    private static boolean eventually(BooleanSupplier condition, int attempts) throws InterruptedException {
        for (int i = 0; i < attempts; i++) {
            if (condition.getAsBoolean()) return true;
            Thread.sleep(100);
        }
        return false;
    }

    private int noticesOf(UUID learner) {
        return jdbc.queryForObject("SELECT count(*) FROM notifications WHERE recipient_id=?", Integer.class, learner);
    }

    @Test
    void aGrantedSubscriptionBecomesOneNoticeAndIsPushedOnce() throws Exception {
        UUID learner = UUID.randomUUID();
        var event = event(learner);

        publish(event, json.writeValueAsBytes(event), event.eventId().toString());
        assertTrue(eventually(() -> noticesOf(learner) == 1), "the notice was stored");
        verify(noticePush, times(1)).pushToUser(eq(learner), any());

        assertEquals(SubscriptionGrantedV1.EVENT_TYPE,
                jdbc.queryForObject("SELECT event_type FROM notifications WHERE recipient_id=?", String.class, learner));

        // The broker delivers at least once: the same event again adds nothing and is not pushed again.
        publish(event, json.writeValueAsBytes(event), event.eventId().toString());
        Thread.sleep(1500);
        assertEquals(1, noticesOf(learner));
        verify(noticePush, times(1)).pushToUser(eq(learner), any());
        assertNull(rabbit.receive(SubscriptionGrantedTopology.DLQ), "a duplicate is not an error");
    }

    @Test
    void aMessageThatCannotBeUsedIsDeadLetteredAndStoresNothing() throws Exception {
        UUID learner = UUID.randomUUID();
        publish(event(learner), "not json".getBytes(StandardCharsets.UTF_8), UUID.randomUUID().toString());
        assertNotNull(rabbit.receive(SubscriptionGrantedTopology.DLQ, 10_000),
                "an unreadable message goes to the dead-letter queue");

        // A readable event whose message id is not its event id breaks the contract.
        var forged = event(learner);
        publish(forged, json.writeValueAsBytes(forged), UUID.randomUUID().toString());
        assertNotNull(rabbit.receive(SubscriptionGrantedTopology.DLQ, 10_000), "a contract break is dead-lettered too");

        assertEquals(0, noticesOf(learner));
        verify(noticePush, times(0)).pushToUser(any(), any(Map.class));
    }

    @Test
    void temporaryFailureIsRetriedThenSucceeds() throws Exception {
        UUID learner = UUID.randomUUID();
        var event = event(learner);
        doThrow(new TransientDataAccessResourceException("temporary database outage"))
                .doCallRealMethod().when(notifications).insertIfAbsent(any(), any());

        publish(event, json.writeValueAsBytes(event), event.eventId().toString());

        assertTrue(eventually(() -> noticesOf(learner) == 1, 450),
                "the message is retried after the temporary database error");
        verify(notifications, times(2)).insertIfAbsent(eq(event.eventId()), any(Notification.class));
        verify(noticePush, times(1)).pushToUser(eq(learner), any());
    }

    @Test
    void aReplayedDeadLetterReachesTheListenerWithTheOriginalRoutingKey() throws Exception {
        UUID learner = UUID.randomUUID();
        var event = event(learner);
        // Parked on the dead-letter queue, as a failed delivery leaves it.
        rabbit.send("", SubscriptionGrantedTopology.DLQ, MessageBuilder.withBody(json.writeValueAsBytes(event))
                .setContentType(MessageProperties.CONTENT_TYPE_JSON).setMessageId(event.eventId().toString()).build());

        var result = replayer.replay(SubscriptionGrantedTopology.DLQ, 10);

        assertEquals(1, result.moved());
        assertTrue(eventually(() -> noticesOf(learner) == 1),
                "the listener received the replay on the original routing key and stored the notice");
    }
}
