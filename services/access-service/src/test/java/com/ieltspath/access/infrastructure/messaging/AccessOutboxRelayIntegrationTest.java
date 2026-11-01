package com.ieltspath.access.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.access.domain.entity.OutboxEvent;
import com.ieltspath.access.domain.repository.OutboxEventRepository;
import com.ieltspath.access.domain.aggregate.Plan;
import com.ieltspath.access.domain.repository.PlanRepository;
import com.ieltspath.access.application.command.GrantSubscriptionCommand;
import com.ieltspath.access.application.usecase.GrantSubscriptionUseCase;
import com.ieltspath.commonobservability.CorrelationIdFilter;
import com.ieltspath.events.access.SubscriptionGrantedTopology;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.slf4j.MDC;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/** The access relay against a real PostgreSQL and a real RabbitMQ; the broker outage is simulated on the publisher. */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {
        "spring.cloud.config.enabled=false", "spring.config.import=", "eureka.client.enabled=false",
        "access.outbox.relay.enabled=false", "access.outbox.relay.confirm-timeout=PT10S",
        "spring.rabbitmq.publisher-confirm-type=correlated", "spring.rabbitmq.publisher-returns=true",
        "spring.rabbitmq.template.mandatory=true",
        "app.auth.internal-jwt-issuer=urn:test:gateway",
        "app.auth.internal-jwt-secret=dGVzdC1vbmx5LWludGVybmFsLWp3dC1zaWduaW5nLWtleQ=="
})
class AccessOutboxRelayIntegrationTest {
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

    @Autowired AccessOutboxRelay relay;
    @Autowired OutboxEventRepository outbox;
    @Autowired RabbitTemplate rabbit;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlanRepository plans;
    @Autowired GrantSubscriptionUseCase grantSubscription;
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired ObjectMapper json;
    @MockitoSpyBean AccessEventPublisher publisher;

    @BeforeEach
    void clean() throws Exception {
        jdbc.execute("TRUNCATE outbox_events");
        drainQueue();
        doCallRealMethod().when(publisher).publish(any());
    }

    private static String payload(UUID eventId) {
        return "{\"eventId\":\"" + eventId + "\",\"eventType\":\"access.subscription-granted.v1\"}";
    }

    private OutboxEvent grant(UUID eventId) {
        OutboxEvent event = OutboxEvent.create(eventId, "Subscription", UUID.randomUUID(), "SubscriptionGranted", payload(eventId));
        outbox.save(event);
        return event;
    }

    private void drainQueue() {
        while (rabbit.receive(SubscriptionGrantedTopology.QUEUE, 200) != null) {
            // Messages left from an earlier test are discarded.
        }
    }

    private List<Message> receiveAll() {
        List<Message> received = new ArrayList<>();
        Message message;
        while ((message = rabbit.receive(SubscriptionGrantedTopology.QUEUE, 1_000)) != null) {
            received.add(message);
        }
        return received;
    }

    private String status(UUID id) {
        return jdbc.queryForObject("SELECT status FROM outbox_events WHERE id = ?", String.class, id);
    }

    private int retryCount(UUID id) {
        return jdbc.queryForObject("SELECT retry_count FROM outbox_events WHERE id = ?", Integer.class, id);
    }

    @Test
    void publishesAPendingEventWithItsIdAndMarksItPublished() throws Exception {
        UUID eventId = UUID.randomUUID();
        grant(eventId);

        assertEquals(1, relay.publishPendingBatch());

        Message message = rabbit.receive(SubscriptionGrantedTopology.QUEUE, 10_000);
        assertNotNull(message, "the event reached the notification queue");
        assertEquals(eventId.toString(), message.getMessageProperties().getMessageId());
        // The payload is stored as JSONB, which may reformat whitespace, so the body is compared as JSON.
        assertEquals(json.readTree(payload(eventId)),
                json.readTree(new String(message.getBody(), StandardCharsets.UTF_8)));
        assertEquals("PUBLISHED", status(eventId));
        assertNull(jdbc.queryForObject("SELECT last_error FROM outbox_events WHERE id = ?", String.class, eventId));
    }

    @Test
    void anEventStaysPendingWhileTheBrokerIsDownAndIsSentWhenItReturns() {
        UUID eventId = UUID.randomUUID();
        grant(eventId);
        doThrow(new IllegalStateException("broker down")).doCallRealMethod().when(publisher).publish(any());

        assertEquals(0, relay.publishPendingBatch());
        assertEquals("PENDING", status(eventId));
        assertEquals(1, retryCount(eventId));

        assertEquals(1, relay.publishPendingBatch());
        assertEquals("PUBLISHED", status(eventId));
        assertEquals(1, receiveAll().size());
    }

    @Test
    void relayOnAnotherThreadKeepsTheRequestCorrelationId() throws Exception {
        MDC.put(CorrelationIdFilter.MDC_KEY, "req-9");
        UUID eventId = UUID.randomUUID();
        grant(eventId);
        MDC.remove(CorrelationIdFilter.MDC_KEY);

        assertEquals(1, CompletableFuture.supplyAsync(relay::publishPendingBatch).get());

        Message message = rabbit.receive(SubscriptionGrantedTopology.QUEUE, 10_000);
        assertNotNull(message, "the outbox row reached the broker from the relay thread");
        assertEquals("req-9", message.getMessageProperties().getHeader("x-correlation-id"));
    }

    @Test
    void twoRelaysRunningAtOnceSendEachEventOnce() throws Exception {
        List<UUID> ids = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            UUID id = UUID.randomUUID();
            ids.add(id);
            grant(id);
        }

        CompletableFuture<Integer> first = CompletableFuture.supplyAsync(relay::publishPendingBatch);
        CompletableFuture<Integer> second = CompletableFuture.supplyAsync(relay::publishPendingBatch);
        first.get();
        second.get();

        List<String> messageIds = receiveAll().stream().map(m -> m.getMessageProperties().getMessageId()).toList();
        assertEquals(ids.size(), messageIds.size());
        assertEquals(ids.size(), new java.util.HashSet<>(messageIds).size(), "no event was sent twice");
    }

    @Test
    void anEventIsGivenUpAfterTheLastAttempt() {
        UUID eventId = UUID.randomUUID();
        grant(eventId);
        jdbc.update("UPDATE outbox_events SET retry_count = 19 WHERE id = ?", eventId);
        doThrow(new IllegalStateException("broker rejected")).when(publisher).publish(any());

        relay.publishPendingBatch();

        assertEquals("FAILED", status(eventId));
        assertEquals(20, retryCount(eventId));
        assertEquals(0, relay.publishPendingBatch(), "a failed event is not claimed again");
        verify(publisher, times(1)).publish(any());
    }

    @Test
    void eventTypesOutsideTheRelayListAreNeverSent() {
        UUID debitId = UUID.randomUUID();
        outbox.save(OutboxEvent.create(debitId, "PointWallet", UUID.randomUUID(), "PointDebited", payload(debitId)));

        assertEquals(0, relay.publishPendingBatch());

        assertEquals("NOT_RELAYED", status(debitId));
        assertEquals(0, receiveAll().size());
    }

    @Test
    void rollbackLeavesNoOutboxRow() {
        UUID learner = UUID.randomUUID();
        Plan plan = plans.save(Plan.create("ROLLBACK-" + UUID.randomUUID(), "Rollback test plan"));

        assertThrows(IllegalStateException.class, () -> new TransactionTemplate(transactionManager)
                .executeWithoutResult(status -> {
                    grantSubscription.execute(new GrantSubscriptionCommand(learner, plan.getId(), 30, 0));
                    throw new IllegalStateException("roll back the subscription grant");
                }));

        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM outbox_events WHERE event_type = ?",
                Integer.class, "SubscriptionGranted"));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM subscriptions WHERE user_id = ?",
                Integer.class, learner));
    }
}
