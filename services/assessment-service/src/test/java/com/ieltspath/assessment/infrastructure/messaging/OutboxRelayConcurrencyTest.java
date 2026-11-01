package com.ieltspath.assessment.infrastructure.messaging;

import com.ieltspath.assessment.domain.entity.OutboxEvent;
import com.ieltspath.assessment.domain.repository.OutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
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

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {
        "spring.cloud.config.enabled=false", "spring.config.import=", "eureka.client.enabled=false",
        "assessment.outbox.relay.enabled=false", "spring.rabbitmq.publisher-confirm-type=correlated",
        "spring.rabbitmq.publisher-returns=true", "spring.rabbitmq.template.mandatory=true",
        "app.auth.internal-jwt-issuer=urn:test:gateway",
        "app.auth.internal-jwt-secret=dGVzdC1vbmx5LWludGVybmFsLWp3dC1zaWduaW5nLWtleQ=="
})
class OutboxRelayConcurrencyTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");
    @Container
    static final GenericContainer<?> RABBITMQ = new GenericContainer<>("rabbitmq:3.13-alpine")
            .withExposedPorts(5672).waitingFor(Wait.forLogMessage(".*Server startup complete.*", 1));

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.rabbitmq.host", RABBITMQ::getHost);
        registry.add("spring.rabbitmq.port", () -> RABBITMQ.getMappedPort(5672));
        registry.add("spring.rabbitmq.username", () -> "guest");
        registry.add("spring.rabbitmq.password", () -> "guest");
        registry.add("spring.rabbitmq.publisher-confirm-type", () -> "correlated");
        registry.add("spring.rabbitmq.publisher-returns", () -> "true");
        registry.add("spring.rabbitmq.template.mandatory", () -> "true");
    }

    @Autowired OutboxRelay relay;
    @Autowired OutboxEventRepository outbox;
    @Autowired JdbcTemplate jdbc;
    @Autowired AmqpAdmin amqpAdmin;
    @Autowired RabbitTemplate rabbitTemplate;
    @Autowired TopicExchange assessmentEventsExchange;

    @BeforeEach
    void clearOutbox() {
        jdbc.execute("TRUNCATE outbox_events");
    }

    @Test
    void twoRelaysRunningAtOnceSendEachEventOnce() throws Exception {
        String queueName = "test.assessment-concurrent." + UUID.randomUUID();
        amqpAdmin.declareQueue(new Queue(queueName, false, false, false));
        amqpAdmin.declareBinding(BindingBuilder.bind(new Queue(queueName)).to(assessmentEventsExchange)
                .with("assessment.completed.v2"));
        List<UUID> eventIds = new ArrayList<>();
        for (int index = 0; index < 20; index++) {
            UUID eventId = UUID.randomUUID();
            eventIds.add(eventId);
            outbox.save(OutboxEvent.pending(eventId, "AssessmentResult", UUID.randomUUID().toString(),
                    "AssessmentCompleted.v2", "{\"event_id\":\"" + eventId + "\"}", Instant.now()));
        }

        try {
            CompletableFuture<Integer> first = CompletableFuture.supplyAsync(relay::publishPendingBatch);
            CompletableFuture<Integer> second = CompletableFuture.supplyAsync(relay::publishPendingBatch);
            first.get();
            second.get();

            List<String> messageIds = receiveAll(queueName).stream()
                    .map(message -> message.getMessageProperties().getMessageId()).toList();
            assertEquals(eventIds.size(), messageIds.size());
            assertEquals(eventIds.size(), new HashSet<>(messageIds).size(), "each claimed event is published once");
        } finally {
            amqpAdmin.deleteQueue(queueName);
        }
    }

    private List<Message> receiveAll(String queueName) {
        List<Message> messages = new ArrayList<>();
        Message message;
        while ((message = rabbitTemplate.receive(queueName, 1_000)) != null) messages.add(message);
        return messages;
    }
}
