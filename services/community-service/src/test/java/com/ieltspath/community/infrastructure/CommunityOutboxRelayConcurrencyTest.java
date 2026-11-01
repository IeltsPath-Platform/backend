package com.ieltspath.community.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.community.infrastructure.messaging.CommunityOutboxRelay;
import com.ieltspath.events.community.CommunityEventTypes;
import com.ieltspath.events.community.CommunityMessaging;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Testcontainers(disabledWithoutDocker = true)
class CommunityOutboxRelayConcurrencyTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");
    @Container
    static final GenericContainer<?> RABBITMQ = new GenericContainer<>(DockerImageName.parse("rabbitmq:3.13-management-alpine"))
            .withExposedPorts(5672).withEnv("RABBITMQ_DEFAULT_USER", "test")
            .withEnv("RABBITMQ_DEFAULT_PASS", "test")
            .waitingFor(Wait.forLogMessage(".*Server startup complete.*", 1));

    @Test
    void twoRelaysRunningAtOnceSendEachEventOnce() throws Exception {
        JdbcTemplate jdbc = outboxTable();
        CachingConnectionFactory connection = new CachingConnectionFactory(RABBITMQ.getHost(), RABBITMQ.getMappedPort(5672));
        connection.setUsername("test");
        connection.setPassword("test");
        connection.setPublisherConfirmType(CachingConnectionFactory.ConfirmType.CORRELATED);
        connection.setPublisherReturns(true);
        RabbitTemplate rabbit = new RabbitTemplate(connection);
        rabbit.setMandatory(true);
        RabbitAdmin admin = new RabbitAdmin(connection);
        String queueName = "community.concurrent." + UUID.randomUUID();
        TopicExchange exchange = new TopicExchange(CommunityMessaging.EXCHANGE, true, false);
        Queue queue = new Queue(queueName, false, false, false);
        admin.declareExchange(exchange);
        admin.declareQueue(queue);
        admin.declareBinding(BindingBuilder.bind(queue).to(exchange).with(CommunityEventTypes.POST_REACTED));
        ObjectMapper mapper = new ObjectMapper().registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        CommunityOutboxRelay firstRelay = relay(jdbc, rabbit, mapper);
        CommunityOutboxRelay secondRelay = relay(jdbc, rabbit, mapper);
        List<UUID> eventIds = new ArrayList<>();
        for (int index = 0; index < 20; index++) {
            UUID eventId = UUID.randomUUID();
            eventIds.add(eventId);
            jdbc.update("INSERT INTO outbox_events(id, event_type, payload, created_at) "
                    + "VALUES (?, ?, ?::jsonb, now())", eventId, CommunityEventTypes.POST_REACTED,
                    "{\"eventId\":\"" + eventId + "\"}");
        }

        try {
            CompletableFuture<Integer> first = CompletableFuture.supplyAsync(firstRelay::publishPendingBatch);
            CompletableFuture<Integer> second = CompletableFuture.supplyAsync(secondRelay::publishPendingBatch);
            first.get();
            second.get();

            List<String> messageIds = receiveAll(rabbit, queueName).stream()
                    .map(message -> message.getMessageProperties().getMessageId()).toList();
            assertEquals(eventIds.size(), messageIds.size());
            assertEquals(eventIds.size(), new HashSet<>(messageIds).size(), "each claimed event is published once");
        } finally {
            admin.deleteQueue(queueName);
            connection.destroy();
        }
    }

    private JdbcTemplate outboxTable() {
        JdbcTemplate jdbc = new JdbcTemplate(new DriverManagerDataSource(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()));
        jdbc.execute("DROP TABLE IF EXISTS outbox_events");
        jdbc.execute("CREATE TABLE outbox_events(id UUID PRIMARY KEY, event_type VARCHAR(100) NOT NULL, "
                + "payload JSONB NOT NULL, created_at TIMESTAMPTZ NOT NULL, published_at TIMESTAMPTZ, "
                + "next_attempt_at TIMESTAMPTZ, retry_count INTEGER NOT NULL DEFAULT 0, last_error TEXT, failed_at TIMESTAMPTZ, "
                + "correlation_id VARCHAR(100))");
        return jdbc;
    }

    private CommunityOutboxRelay relay(JdbcTemplate jdbc, RabbitTemplate rabbit, ObjectMapper mapper) {
        return new CommunityOutboxRelay(jdbc, rabbit, mapper, CommunityMessaging.EXCHANGE,
                20, Duration.ofSeconds(5), true, 12, Duration.ofSeconds(60));
    }

    private List<Message> receiveAll(RabbitTemplate rabbit, String queueName) {
        List<Message> messages = new ArrayList<>();
        Message message;
        while ((message = rabbit.receive(queueName, 1_000)) != null) messages.add(message);
        return messages;
    }
}
