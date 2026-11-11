package com.ieltspath.notification.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.events.access.AccessMessaging;
import com.ieltspath.events.access.SubscriptionExpiryNoticeV1;
import com.ieltspath.notification.application.NotificationPreferenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.UUID;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {
        "spring.cloud.config.enabled=false", "spring.config.import=", "eureka.client.enabled=false",
        "app.auth.internal-jwt-issuer=urn:test:gateway",
        "app.auth.internal-jwt-secret=dGVzdC1vbmx5LWludGVybmFsLWp3dC1zaWduaW5nLWtleQ=="
})
class SubscriptionExpiryInboxBrokerIntegrationTest {
    @Container static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");
    @Container static final GenericContainer<?> RABBIT = new GenericContainer<>("rabbitmq:3.13-management-alpine")
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
    @Autowired NotificationPreferenceService preferences;
    @MockitoBean NoticePushBridge noticePush;

    @BeforeEach void clean() { jdbc.update("DELETE FROM notifications"); jdbc.update("DELETE FROM notification_preferences"); }

    @Test
    void accessExpiryEventCreatesOneInboxNoticeAndRespectsPreference() throws Exception {
        UUID user = UUID.randomUUID();
        var event = new SubscriptionExpiryNoticeV1(UUID.randomUUID(), SubscriptionExpiryNoticeV1.EVENT_TYPE,
                Instant.now(), user, UUID.randomUUID(), "SEVEN_DAYS", Instant.now().plusSeconds(604800),
                "redacted@example.invalid", "Learner");
        publish(event);
        assertTrue(eventually(() -> count(user) == 1));
        verify(noticePush, times(1)).pushToUser(eq(user), any());

        preferences.update(user, event.eventType(), false);
        var suppressed = new SubscriptionExpiryNoticeV1(UUID.randomUUID(), event.eventType(), Instant.now(), user,
                event.subscriptionId(), "EXPIRED", Instant.now(), event.email(), event.displayName());
        publish(suppressed);
        Thread.sleep(500);
        assertEquals(1, count(user));
        verify(noticePush, times(1)).pushToUser(eq(user), any());
    }

    private void publish(SubscriptionExpiryNoticeV1 event) throws Exception {
        rabbit.send(AccessMessaging.EXCHANGE, event.eventType(), MessageBuilder.withBody(json.writeValueAsBytes(event))
                .setContentType(MessageProperties.CONTENT_TYPE_JSON).setMessageId(event.eventId().toString()).build());
    }

    private int count(UUID user) {
        return jdbc.queryForObject("SELECT count(*) FROM notifications WHERE recipient_id=?", Integer.class, user);
    }

    private static boolean eventually(BooleanSupplier condition) throws InterruptedException {
        for (int i = 0; i < 100; i++) { if (condition.getAsBoolean()) return true; Thread.sleep(100); }
        return false;
    }
}
