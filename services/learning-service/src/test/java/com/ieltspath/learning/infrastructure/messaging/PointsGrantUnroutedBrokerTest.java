package com.ieltspath.learning.infrastructure.messaging;

import com.ieltspath.learning.domain.repository.RewardGrantRepository;
import com.ieltspath.learning.infrastructure.scheduler.RewardGrantRelayJob;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Message;
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
 * The relay against a real RabbitMQ: only Access declares the grant queue, so a grant sent before it exists is dropped by
 * a plain publish. With confirms and returns the grant stays pending without using up a send, and is delivered once the
 * queue is bound.
 */
@SpringBootTest(properties = {
        "spring.config.import=", "spring.cloud.config.enabled=false", "eureka.client.enabled=false",
        "spring.jpa.hibernate.ddl-auto=validate", "spring.jpa.open-in-view=false",
        "management.health.rabbit.enabled=false", "learning.messaging.consumer-enabled=false",
        "learning.mastery.backfill-on-startup=false", "learning.league.close-enabled=false",
        "learning.rewards.relay-enabled=false", "app.auth.internal-jwt-issuer=urn:code-base:api-gateway"
})
@Testcontainers(disabledWithoutDocker = true)
class PointsGrantUnroutedBrokerTest {
    private static final String GRANT_QUEUE = "access.points-grant";

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

    @BeforeEach
    void clean() {
        jdbc.execute("TRUNCATE reward_grants");
        admin.deleteQueue(GRANT_QUEUE);
    }

    @Test
    void aGrantSentBeforeAccessDeclaredItsQueueStaysPendingAndIsDeliveredOnceTheQueueIsBound() {
        UUID user = UUID.randomUUID();
        grants.enqueue(user, "DAILY_QUEST", "quest:1", 5);

        assertEquals(0, relay.runOnce(), "nothing took the grant");
        assertEquals("PENDING", status());
        assertEquals(0, attempts(), "a grant nobody received does not use up a send");

        Queue queue = new Queue(GRANT_QUEUE, true);
        admin.declareQueue(queue);
        admin.declareBinding(BindingBuilder.bind(queue)
                .to(new TopicExchange(RewardGrantMessagingConfig.EXCHANGE, true, false))
                .with(RewardGrantMessagingConfig.REQUESTED_ROUTING_KEY));

        assertEquals(1, relay.runOnce());
        assertEquals(1, attempts());
        Message delivered = rabbitTemplate.receive(GRANT_QUEUE, 5_000);
        assertNotNull(delivered, "the grant reached the queue");
        assertTrue(new String(delivered.getBody(), StandardCharsets.UTF_8).contains(user.toString()));
    }

    private int attempts() {
        Integer value = jdbc.queryForObject("SELECT attempts FROM reward_grants", Integer.class);
        return value == null ? 0 : value;
    }

    private String status() {
        return jdbc.queryForObject("SELECT status FROM reward_grants", String.class);
    }
}
