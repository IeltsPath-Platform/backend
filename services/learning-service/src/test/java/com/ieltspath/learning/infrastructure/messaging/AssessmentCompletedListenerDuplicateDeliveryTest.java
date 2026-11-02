package com.ieltspath.learning.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ieltspath.learning.application.port.AccessClient;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import com.rabbitmq.client.Channel;

import java.sql.Timestamp;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@SpringBootTest(properties = {
        "spring.config.import=",
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false",
        "spring.rabbitmq.listener.simple.auto-startup=false",
        "management.health.rabbit.enabled=false",
        "app.auth.internal-jwt-issuer=urn:code-base:api-gateway"
})
@Testcontainers(disabledWithoutDocker = true)
class AssessmentCompletedListenerDuplicateDeliveryTest {
    private static final byte[] KEY = new byte[32];

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("learning_db").withPassword(UUID.randomUUID().toString());

    static {
        new java.security.SecureRandom().nextBytes(KEY);
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("app.auth.internal-jwt-secret", () -> java.util.Base64.getEncoder().encodeToString(KEY));
    }

    @Autowired AssessmentCompletedListener listener;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @MockitoBean AccessClient access;

    @Test
    void secondDeliveryHasNoSideEffect() throws Exception {
        ObjectNode event = (ObjectNode) json.readTree(AssessmentCompletedParserTest.event(""));
        ObjectNode data = (ObjectNode) event.get("data");
        UUID userId = UUID.fromString(data.path("user_id").asText());
        UUID attemptId = UUID.fromString(data.path("attempt_id").asText());
        data.put("assessment_type", "PLACEMENT");
        data.putArray("item_results");

        byte[] body = json.writeValueAsBytes(event);
        String messageId = event.path("event_id").asText();
        Channel channel = mock(Channel.class);

        listener.onMessage(delivery(body, messageId, 1), channel);
        Timestamp firstProcessedAt = jdbc.queryForObject("""
                SELECT processed_at FROM assessment_result_versions WHERE user_id = ? AND attempt_id = ?
                """, Timestamp.class, userId, attemptId);
        listener.onMessage(delivery(body, messageId, 2), channel);

        assertEquals(1, jdbc.queryForObject("""
                SELECT count(*) FROM assessment_result_versions WHERE user_id = ? AND attempt_id = ?
                """, Integer.class, userId, attemptId));
        assertEquals(firstProcessedAt, jdbc.queryForObject("""
                SELECT processed_at FROM assessment_result_versions WHERE user_id = ? AND attempt_id = ?
                """, Timestamp.class, userId, attemptId));
        verify(channel).basicAck(1, false);
        verify(channel).basicAck(2, false);
    }

    private static Message delivery(byte[] body, String messageId, long tag) {
        MessageProperties properties = new MessageProperties();
        properties.setMessageId(messageId);
        properties.setDeliveryTag(tag);
        return new Message(body, properties);
    }
}
