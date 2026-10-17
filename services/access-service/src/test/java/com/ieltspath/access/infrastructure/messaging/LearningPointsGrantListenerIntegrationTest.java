package com.ieltspath.access.infrastructure.messaging;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@Testcontainers
@SpringBootTest(properties = {
        "spring.cloud.config.enabled=false",
        "spring.rabbitmq.listener.simple.auto-startup=false",
        "app.auth.internal-jwt-issuer=urn:code-base:api-gateway"
})
class LearningPointsGrantListenerIntegrationTest {
    private static final String INTERNAL_SECRET = "ZmVkY2JhOTg3NjU0MzIxMGZlZGNiYTk4NzY1NDMyMTA=";

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("app.auth.internal-jwt-secret", () -> INTERNAL_SECRET);
    }

    @Autowired LearningPointsGrantListener listener;
    @Autowired JdbcTemplate jdbc;
    @MockitoBean RabbitTemplate rabbitTemplate;

    @Test
    void grantsOnceWhenTheSameGrantIsDeliveredAgain() {
        UUID grantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Message message = grantMessage(grantId, userId, 4, "STREAK_MILESTONE", "streak:2026-10-01:7");

        listener.onPointsGrantRequested(message);
        listener.onPointsGrantRequested(message);

        assertEquals(4L, balance(userId));
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM point_ledger_entries WHERE idempotency_key = ?",
                Integer.class, grantId.toString()));
        assertSettlements(grantId, "GRANTED", 2);
    }

    @Test
    void reportsCappedWhenTheDailyCreditLimitIsReached() {
        UUID grantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        listener.onPointsGrantRequested(grantMessage(UUID.randomUUID(), userId, 19,
                "STREAK_MILESTONE", "streak:2026-10-02:7"));
        reset(rabbitTemplate);

        listener.onPointsGrantRequested(grantMessage(grantId, userId, 3,
                "STREAK_MILESTONE", "streak:2026-10-02:14"));

        assertEquals(20L, balance(userId));
        assertSettlements(grantId, "CAPPED", 1);
    }

    @Test
    void reportsInvalidPayloadAsFailedWithoutCrediting() {
        UUID grantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        listener.onPointsGrantRequested(grantMessage(grantId, userId, 101,
                "STREAK_MILESTONE", "streak:2026-10-03:7"));

        assertEquals(0L, balance(userId));
        assertSettlements(grantId, "FAILED", 1);
    }

    private long balance(UUID userId) {
        return jdbc.queryForObject("SELECT COALESCE(MAX(balance), 0) FROM point_wallets WHERE user_id = ?",
                Long.class, userId);
    }

    private void assertSettlements(UUID grantId, String status, int count) {
        ArgumentCaptor<PointsGrantSettled> settlement = ArgumentCaptor.forClass(PointsGrantSettled.class);
        verify(rabbitTemplate, times(count)).convertAndSend(
                eq(LearningPointsMessagingConfig.EXCHANGE),
                eq(LearningPointsMessagingConfig.SETTLED_ROUTING_KEY), settlement.capture());
        settlement.getAllValues().forEach(value -> {
            assertEquals(grantId, value.grantId());
            assertEquals(status, value.status());
        });
    }

    private static Message grantMessage(UUID grantId, UUID userId, int amount, String reason, String referenceId) {
        String json = "{\"grantId\":\"" + grantId + "\",\"userId\":\"" + userId
                + "\",\"amount\":" + amount + ",\"reason\":\"" + reason
                + "\",\"referenceId\":\"" + referenceId + "\"}";
        return new Message(json.getBytes(StandardCharsets.UTF_8));
    }
}
