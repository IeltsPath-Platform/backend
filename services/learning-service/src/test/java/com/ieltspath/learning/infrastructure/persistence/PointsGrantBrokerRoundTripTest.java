package com.ieltspath.learning.infrastructure.persistence;

import com.ieltspath.events.points.PointsGrantMessaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.learning.domain.repository.RewardGrantRepository;
import com.ieltspath.learning.infrastructure.messaging.RewardGrantMessagingConfig;
import com.ieltspath.learning.infrastructure.scheduler.RewardGrantRelayJob;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
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
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The points grant leaves learning on a real RabbitMQ and Access's answer comes back to it. The Access side is played by a
 * queue bound the way Access binds {@code access.points-grant} and by messages shaped like the ones Access sends (JSON with
 * a type header); the real Access consumer is covered by its own tests. Confirms what mocks cannot: exchange and queue
 * declarations, routing keys, the JSON on the wire and the listener reading an answer.
 */
@SpringBootTest(properties = {
        "spring.config.import=", "spring.cloud.config.enabled=false", "eureka.client.enabled=false",
        "spring.jpa.hibernate.ddl-auto=validate", "spring.jpa.open-in-view=false",
        "management.health.rabbit.enabled=false", "learning.messaging.consumer-enabled=true",
        "learning.mastery.backfill-on-startup=false", "learning.league.close-enabled=false",
        "learning.rewards.relay-enabled=false", "app.auth.internal-jwt-issuer=urn:code-base:api-gateway"
})
@Testcontainers(disabledWithoutDocker = true)
class PointsGrantBrokerRoundTripTest {
    private static final String ACCESS_QUEUE = "test.access-points-grant";

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

    @Autowired JdbcTemplate jdbc;
    @Autowired RewardGrantRepository grants;
    @Autowired RewardGrantRelayJob relay;
    @Autowired RabbitTemplate rabbitTemplate;
    @Autowired RabbitAdmin admin;
    @Autowired ObjectMapper json;

    private final UUID user = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        jdbc.execute("TRUNCATE reward_grants");
        TopicExchange exchange = new TopicExchange(PointsGrantMessaging.EXCHANGE, true, false);
        Queue access = new Queue(ACCESS_QUEUE, false);
        admin.declareExchange(exchange);
        admin.declareQueue(access);
        admin.purgeQueue(ACCESS_QUEUE, false);
        admin.declareBinding(BindingBuilder.bind(access).to(exchange)
                .with(PointsGrantMessaging.REQUESTED_ROUTING_KEY));
    }

    @Test
    void aGrantReachesAccessAsTheAgreedJsonAndItsAnswerSettlesIt() throws Exception {
        grants.enqueue(user, "DAILY_QUEST", "PRACTICE:2026-10-18", 5);

        assertEquals(1, relay.runOnce());

        Message sent = rabbitTemplate.receive(ACCESS_QUEUE, 10_000);
        assertNotNull(sent, "the grant was published to the requested routing key");
        JsonNode body = json.readTree(sent.getBody());
        UUID grantId = UUID.fromString(body.path("grantId").asText());
        assertEquals(user.toString(), body.path("userId").asText());
        assertEquals(5, body.path("amount").asInt());
        assertEquals("DAILY_QUEST", body.path("reason").asText());
        assertEquals("PRACTICE:2026-10-18", body.path("referenceId").asText());
        assertEquals(grantId.toString(), sent.getMessageProperties().getMessageId());
        assertEquals(MessageProperties.CONTENT_TYPE_JSON, sent.getMessageProperties().getContentType());

        answer(grantId, "GRANTED", null);

        awaitStatus(grantId, "GRANTED");
    }

    @Test
    void aFailedAnswerKeepsItsErrorAndAnUnreadableAnswerDoesNotBlockTheQueue() throws Exception {
        grants.enqueue(user, "BADGE", "STREAK_7", 5);
        relay.runOnce();
        Message sent = rabbitTemplate.receive(ACCESS_QUEUE, 10_000);
        assertNotNull(sent);
        UUID grantId = UUID.fromString(json.readTree(sent.getBody()).path("grantId").asText());

        // Garbage first: it is dropped, and the real answer behind it is still read.
        rabbitTemplate.send(PointsGrantMessaging.EXCHANGE, PointsGrantMessaging.SETTLED_ROUTING_KEY,
                MessageBuilder.withBody("not json".getBytes(StandardCharsets.UTF_8)).build());
        answer(grantId, "FAILED", "INVALID_PAYLOAD");

        awaitStatus(grantId, "FAILED");
        assertEquals("INVALID_PAYLOAD", jdbc.queryForObject("SELECT last_error FROM reward_grants WHERE id = ?",
                String.class, grantId));
    }

    /** An answer shaped like Access's: JSON with the keys {@code grantId}, {@code status}, {@code error}. */
    private void answer(UUID grantId, String status, String error) throws Exception {
        String payload = json.writeValueAsString(new java.util.LinkedHashMap<String, Object>() {{
            put("grantId", grantId);
            put("status", status);
            put("error", error);
        }});
        Message answer = MessageBuilder.withBody(payload.getBytes(StandardCharsets.UTF_8))
                .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                .setHeader("__TypeId__", "com.ieltspath.access.infrastructure.messaging.PointsGrantSettled").build();
        rabbitTemplate.send(PointsGrantMessaging.EXCHANGE, PointsGrantMessaging.SETTLED_ROUTING_KEY, answer);
    }

    private void awaitStatus(UUID grantId, String expected) throws InterruptedException {
        String status = null;
        for (int tries = 0; tries < 100; tries++) {
            status = jdbc.queryForObject("SELECT status FROM reward_grants WHERE id = ?", String.class, grantId);
            if (expected.equals(status)) break;
            Thread.sleep(100);
        }
        assertTrue(expected.equals(status), "the grant is " + expected + " but is " + status);
    }
}
