package com.ieltspath.notification.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.events.community.CommunityEvent;
import com.ieltspath.events.community.CommunityEventTypes;
import com.ieltspath.events.community.CommunityMessaging;
import com.ieltspath.events.messaging.DeadLetterReplayer;
import com.ieltspath.events.messaging.DeadLetterRoute;
import com.ieltspath.notification.application.NotificationPreferenceService;
import com.ieltspath.notification.application.RecordCommunityEventUseCase;
import com.ieltspath.notification.infrastructure.persistence.NotificationPreferenceRepositoryAdapter;
import com.ieltspath.notification.infrastructure.persistence.NotificationRepositoryAdapter;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.support.DefaultMessagePropertiesConverter;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * A community event the listener rejects is parked on the dead-letter queue, and a replay sends it back on its original
 * routing key, so the consumer accepts it on the second delivery. Runs against a real RabbitMQ and PostgreSQL, driving the
 * consumer one delivery at a time the way {@link CommunityEventConsumerIntegrationTest} does.
 */
@Testcontainers(disabledWithoutDocker = true)
class CommunityReplayBrokerIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");
    @Container
    static final GenericContainer<?> RABBIT = new GenericContainer<>("rabbitmq:3.13-management-alpine")
            .withExposedPorts(5672).waitingFor(Wait.forLogMessage(".*Server startup complete.*", 1));

    @Test
    void aParkedCommunityEventIsReplayedOnItsOriginalRoutingKey() throws Exception {
        var source = new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        org.flywaydb.core.Flyway.configure().dataSource(source).locations("filesystem:src/main/resources/db/migration")
                .load().migrate();
        var jdbc = new JdbcTemplate(source);
        var mapper = new ObjectMapper().registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())
                .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        var connection = new CachingConnectionFactory(RABBIT.getHost(), RABBIT.getMappedPort(5672));
        connection.setPublisherConfirmType(CachingConnectionFactory.ConfirmType.CORRELATED);
        connection.setPublisherReturns(true);
        try {
            var rabbit = new RabbitTemplate(connection);
            rabbit.setMandatory(true);
            var admin = new RabbitAdmin(connection);
            var topology = new NotificationMessagingConfig(new StandardEnvironment());
            admin.declareExchange(topology.communityExchange());
            admin.declareQueue(topology.notificationQueue());
            admin.declareBinding(topology.notificationBinding());
            admin.declareExchange(topology.deadLetterExchange());
            admin.declareQueue(topology.notificationDlq());
            admin.declareBinding(topology.notificationDlqBinding());

            var recordEvent = spy(new RecordCommunityEventUseCase(new NotificationRepositoryAdapter(jdbc),
                    new NotificationPreferenceService(new NotificationPreferenceRepositoryAdapter(jdbc))));
            var noticePush = mock(NoticePushBridge.class);
            var consumer = new CommunityEventConsumer(mapper, recordEvent, noticePush, rabbit);
            var replayer = new DeadLetterReplayer(connection, admin, List.of(DeadLetterRoute.byOriginalRoutingKey(
                    CommunityMessaging.NOTIFICATIONS_DLQ, CommunityMessaging.NOTIFICATIONS_QUEUE, CommunityMessaging.EXCHANGE)));

            UUID recipient = UUID.randomUUID();
            var event = new CommunityEvent(UUID.randomUUID(), CommunityEventTypes.USER_FOLLOWED, Instant.now(),
                    UUID.randomUUID(), recipient, null, null);
            // The first delivery is rejected, as a contract failure would be; the second uses the real use case.
            doThrow(new IllegalArgumentException("rejected once")).doCallRealMethod()
                    .when(recordEvent).execute(any(), any(), any());
            var properties = new MessageProperties();
            properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
            properties.setContentEncoding(StandardCharsets.UTF_8.name());
            properties.setMessageId(event.eventId().toString());
            rabbit.send(CommunityMessaging.EXCHANGE, event.eventType(),
                    new Message(mapper.writeValueAsBytes(event), properties));

            deliverOnce(rabbit, consumer, CommunityMessaging.NOTIFICATIONS_QUEUE);

            assertEquals(1, messagesIn(rabbit, CommunityMessaging.NOTIFICATIONS_DLQ), "the rejected event was parked");
            assertEquals(0, noticesOf(jdbc, recipient));

            var result = replayer.replay(CommunityMessaging.NOTIFICATIONS_DLQ, 10);

            assertEquals(1, result.moved());
            deliverOnce(rabbit, consumer, CommunityMessaging.NOTIFICATIONS_QUEUE);
            assertEquals(1, noticesOf(jdbc, recipient), "the replayed event was accepted on its original routing key");
            verify(recordEvent, times(2))
                    .execute(any(), eq(CommunityEventTypes.USER_FOLLOWED), eq(event.eventId().toString()));
            verify(noticePush, times(1)).pushToUser(eq(recipient), any());
        } finally {
            connection.destroy();
        }
    }

    /** Takes one message from the queue on a channel and hands it to the consumer, as the listener would. */
    private static void deliverOnce(RabbitTemplate rabbit, CommunityEventConsumer consumer, String queue) {
        rabbit.execute(channel -> {
            var delivery = channel.basicGet(queue, false);
            assertNotNull(delivery, "a message is waiting on " + queue);
            var deliveredProperties = new DefaultMessagePropertiesConverter()
                    .toMessageProperties(delivery.getProps(), delivery.getEnvelope(), "UTF-8");
            try {
                consumer.receive(new Message(delivery.getBody(), deliveredProperties), channel);
            } catch (Exception failure) {
                throw new IllegalStateException(failure);
            }
            return null;
        });
    }

    private static long messagesIn(RabbitTemplate rabbit, String queue) {
        return rabbit.execute(channel -> channel.messageCount(queue));
    }

    private static int noticesOf(JdbcTemplate jdbc, UUID recipient) {
        return jdbc.queryForObject("SELECT count(*) FROM notifications WHERE recipient_id=?", Integer.class, recipient);
    }
}
