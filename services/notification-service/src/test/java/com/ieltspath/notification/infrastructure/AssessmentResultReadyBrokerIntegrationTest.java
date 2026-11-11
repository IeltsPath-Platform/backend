package com.ieltspath.notification.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.events.assessment.AssessmentCompletedV2;
import com.ieltspath.events.assessment.AssessmentMessaging;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
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
class AssessmentResultReadyBrokerIntegrationTest {
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
    void firstPlacementAndMockResultsAreStoredAndRegradesAreIgnored() throws Exception {
        UUID user = UUID.randomUUID();
        var placement = event(user, "PLACEMENT", 1);
        publish(placement);
        assertTrue(eventually(() -> count(user) == 1));
        verify(noticePush).pushToUser(eq(user), any());

        publish(event(user, "MOCK", 1));
        assertTrue(eventually(() -> count(user) == 2));
        publish(event(user, "MOCK", 2));
        Thread.sleep(500);
        assertEquals(2, count(user));
        verify(noticePush, times(2)).pushToUser(eq(user), any());

        preferences.update(user, "assessment.result-ready.v1", false);
        publish(event(user, "PLACEMENT", 1));
        Thread.sleep(500);
        assertEquals(2, count(user));
    }

    private AssessmentCompletedV2 event(UUID user, String type, int version) {
        return new AssessmentCompletedV2(UUID.randomUUID(), AssessmentCompletedV2.EVENT_TYPE, Instant.now(),
                AssessmentCompletedV2.SOURCE, new AssessmentCompletedV2.Data(user, null, UUID.randomUUID(),
                UUID.randomUUID(), UUID.randomUUID(), version, type, "COMPLETED", Instant.now(), null,
                List.of(new AssessmentCompletedV2.ItemResult(UUID.randomUUID(), UUID.randomUUID(), true,
                        BigDecimal.ONE, BigDecimal.ONE, List.of()))));
    }

    private void publish(AssessmentCompletedV2 event) throws Exception {
        rabbit.send(AssessmentMessaging.EXCHANGE, AssessmentMessaging.COMPLETED_ROUTING_KEY,
                MessageBuilder.withBody(json.writeValueAsBytes(event)).setContentType(MessageProperties.CONTENT_TYPE_JSON)
                        .setMessageId(event.eventId().toString()).build());
    }

    private int count(UUID user) {
        return jdbc.queryForObject("SELECT count(*) FROM notifications WHERE recipient_id=?", Integer.class, user);
    }

    private static boolean eventually(BooleanSupplier condition) throws InterruptedException {
        for (int i = 0; i < 100; i++) { if (condition.getAsBoolean()) return true; Thread.sleep(100); }
        return false;
    }
}
