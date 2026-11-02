package com.ieltspath.notification.infrastructure;

import com.ieltspath.events.community.CommunityEvent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.events.community.CommunityEventTypes;
import com.ieltspath.notification.application.NotificationPreferenceService;
import com.ieltspath.notification.application.RecordCommunityEventUseCase;
import com.ieltspath.notification.infrastructure.persistence.NotificationPreferenceRepositoryAdapter;
import com.ieltspath.notification.infrastructure.persistence.NotificationRepositoryAdapter;
import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@Testcontainers(disabledWithoutDocker = true)
class CommunityEventConsumerIntegrationTest {
    @Container static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");
    @Container static final org.testcontainers.containers.GenericContainer<?> RABBIT =
            new org.testcontainers.containers.GenericContainer<>("rabbitmq:3.13-management-alpine")
                    .withExposedPorts(5672)
                    .waitingFor(org.testcontainers.containers.wait.strategy.Wait.forLogMessage(".*Server startup complete.*", 1));

    @Test
    void consumesTheSharedRecordDeliveredByRabbitAndAcknowledgesTheOriginalDelivery() throws Exception {
        var source = new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        org.flywaydb.core.Flyway.configure().dataSource(source).locations("filesystem:src/main/resources/db/migration").load().migrate();
        var jdbc = new JdbcTemplate(source);
        var socket = mock(NoticePushBridge.class);
        var mapper = new ObjectMapper().registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())
                .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        var connection = new org.springframework.amqp.rabbit.connection.CachingConnectionFactory(
                RABBIT.getHost(), RABBIT.getMappedPort(5672));
        try {
            var rabbit = new RabbitTemplate(connection);
            var admin = new org.springframework.amqp.rabbit.core.RabbitAdmin(connection);
            var topology = new NotificationMessagingConfig(new org.springframework.core.env.StandardEnvironment());
            admin.declareExchange(topology.communityExchange());
            admin.declareQueue(topology.notificationQueue());
            admin.declareBinding(topology.notificationBinding());
            var event = new CommunityEvent(UUID.randomUUID(), CommunityEventTypes.USER_FOLLOWED,
                    Instant.parse("2026-10-01T09:10:00Z"), UUID.randomUUID(), UUID.randomUUID(), null, null);
            var properties = new MessageProperties();
            properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
            properties.setMessageId(event.eventId().toString());
            rabbit.send(com.ieltspath.events.community.CommunityMessaging.EXCHANGE, event.eventType(),
                    new Message(mapper.writeValueAsBytes(event), properties));
            var consumer = new CommunityEventConsumer(mapper, recordEvent(jdbc), socket, rabbit);
            rabbit.execute(channel -> {
                var delivery = channel.basicGet(topology.notificationQueue().getName(), false);
                org.junit.jupiter.api.Assertions.assertNotNull(delivery);
                var deliveredProperties = new org.springframework.amqp.rabbit.support.DefaultMessagePropertiesConverter()
                        .toMessageProperties(delivery.getProps(), delivery.getEnvelope(), "UTF-8");
                try {
                    consumer.receive(new Message(delivery.getBody(), deliveredProperties), channel);
                } catch (Exception failure) {
                    throw new IllegalStateException(failure);
                }
                // Acknowledged deliveries cannot be requeued by recovery on the same channel.
                channel.basicRecover(true);
                org.junit.jupiter.api.Assertions.assertNull(channel.basicGet(topology.notificationQueue().getName(), true));
                return null;
            });
            assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM notifications WHERE id=?", Integer.class, event.eventId()));
            verify(socket).pushToUser(eq(event.recipientId()), any());
        } finally {
            connection.destroy();
        }
    }

    @Test void duplicateDeliveryCreatesOneMentionNotificationAndOnePush() throws Exception {
        var source = new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        org.flywaydb.core.Flyway.configure().dataSource(source).locations("filesystem:src/main/resources/db/migration").load().migrate();
        var jdbc = new JdbcTemplate(source);
        var socket = mock(NoticePushBridge.class);
        var mapper = new ObjectMapper();
        var consumer = new CommunityEventConsumer(mapper, recordEvent(jdbc), socket, mock(RabbitTemplate.class));
        UUID event=UUID.randomUUID(), actor=UUID.randomUUID(), recipient=UUID.randomUUID(), post=UUID.randomUUID(), comment=UUID.randomUUID();
        byte[] body=eventJson(event,CommunityEventTypes.COMMENT_MENTIONED,actor,recipient,post,comment)
                .getBytes(java.nio.charset.StandardCharsets.UTF_8);
        Channel channel=mock(Channel.class);
        for (int i=0;i<2;i++) {
            var properties=new MessageProperties(); properties.setReceivedRoutingKey(CommunityEventTypes.COMMENT_MENTIONED);
            properties.setMessageId(event.toString()); properties.setDeliveryTag(i+1);
            consumer.receive(new Message(body, properties), channel);
        }
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM notifications WHERE comment_id=? AND recipient_id=?", Integer.class, comment, recipient));
        verify(socket, times(1)).pushToUser(eq(recipient), any());
        verify(channel, times(2)).basicAck(anyLong(), eq(false));
    }

    @Test void everyContractEventTypeIsConsumedAndDisabledTypeIsNeitherStoredNorPushed() throws Exception {
        var source = new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        org.flywaydb.core.Flyway.configure().dataSource(source).locations("filesystem:src/main/resources/db/migration").load().migrate();
        var jdbc = new JdbcTemplate(source);
        var socket = mock(NoticePushBridge.class);
        var preferences = new NotificationPreferenceService(new NotificationPreferenceRepositoryAdapter(jdbc));
        var mapper = new ObjectMapper();
        var consumer = new CommunityEventConsumer(mapper, recordEvent(jdbc), socket, mock(RabbitTemplate.class));
        UUID actor=UUID.randomUUID(), recipient=UUID.randomUUID(), post=UUID.randomUUID(), comment=UUID.randomUUID();
        preferences.update(recipient, CommunityEventTypes.COMMENT_MENTIONED, false);
        Channel channel=mock(Channel.class);
        int deliveryTag=0;
        for (String eventType : CommunityEventTypes.ORDERED) {
            UUID event=UUID.randomUUID();
            UUID eventPost=CommunityEventTypes.USER_FOLLOWED.equals(eventType)?null:post;
            UUID eventComment=eventType.equals(CommunityEventTypes.POST_COMMENTED)
                    || eventType.equals(CommunityEventTypes.COMMENT_REPLIED)
                    || eventType.equals(CommunityEventTypes.COMMENT_MENTIONED) ? comment : null;
            var properties=new MessageProperties(); properties.setReceivedRoutingKey(eventType);
            properties.setMessageId(event.toString()); properties.setDeliveryTag(++deliveryTag);
            byte[] body=eventJson(event,eventType,actor,recipient,eventPost,eventComment)
                    .getBytes(java.nio.charset.StandardCharsets.UTF_8);
            consumer.receive(new Message(body,properties),channel);
        }
        assertEquals(CommunityEventTypes.ORDERED.size()-1,
                jdbc.queryForObject("SELECT count(*) FROM notifications WHERE recipient_id=?",Integer.class,recipient));
        assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM notifications WHERE recipient_id=? AND event_type=?",
                Integer.class,recipient,CommunityEventTypes.COMMENT_MENTIONED));
        verify(socket,times(CommunityEventTypes.ORDERED.size()-1))
                .pushToUser(eq(recipient),any());
        verify(channel,times(CommunityEventTypes.ORDERED.size())).basicAck(anyLong(),eq(false));
    }

    private static RecordCommunityEventUseCase recordEvent(JdbcTemplate jdbc) {
        return new RecordCommunityEventUseCase(new NotificationRepositoryAdapter(jdbc),
                new NotificationPreferenceService(new NotificationPreferenceRepositoryAdapter(jdbc)));
    }

    private static String eventJson(UUID event, String eventType, UUID actor, UUID recipient, UUID post, UUID comment) {
        String postValue=post==null?"null":"\""+post+"\"";
        String commentValue=comment==null?"null":"\""+comment+"\"";
        return """
                {"eventId":"%s","eventType":"%s","occurredAt":"%s","actorId":"%s","recipientId":"%s","postId":%s,"commentId":%s}
                """.formatted(event,eventType,Instant.now(),actor,recipient,postValue,commentValue).trim();
    }
}
