package com.ieltspath.access.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.ieltspath.access.application.port.DeletedUserStore;
import com.ieltspath.access.application.usecase.CreditPointsUseCase;
import com.ieltspath.access.application.usecase.RecordUserDeletionUseCase;
import com.ieltspath.events.messaging.MessageRetry;
import com.ieltspath.events.points.PointsGrantRequested;
import com.ieltspath.events.user.UserDeletedV1;
import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class UserDeletionAccessTest {

    @Test
    void listenerRecordsDeletionAndAcks() throws Exception {
        RecordUserDeletionUseCase recordDeletion = mock(RecordUserDeletionUseCase.class);
        ObjectMapper mapper = new ObjectMapper().registerModule(
                new SimpleModule().addSerializer(Instant.class, ToStringSerializer.instance))
                .findAndRegisterModules();
        UserDeletedListener listener = new UserDeletedListener(mapper, recordDeletion, 5);

        UUID eventId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant occurredAt = Instant.parse("2026-11-26T05:00:00Z");
        byte[] body = mapper.writeValueAsBytes(new UserDeletedV1(eventId, UserDeletedV1.EVENT_TYPE, occurredAt, userId));
        MessageProperties props = new MessageProperties();
        props.setDeliveryTag(11L);
        Channel channel = mock(Channel.class);

        listener.onMessage(new Message(body, props), channel);

        verify(recordDeletion).execute(eventId, userId, occurredAt);
        verify(channel).basicAck(11L, false);
    }

    @Test
    void learningPointsGrantListenerSkipsDeletedUserByTombstone() throws Exception {
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        CreditPointsUseCase creditPoints = mock(CreditPointsUseCase.class);
        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
        MessageRetry retry = mock(MessageRetry.class);
        DeletedUserStore deletedUsers = mock(DeletedUserStore.class);

        UUID userId = UUID.randomUUID();
        when(deletedUsers.isDeleted(userId)).thenReturn(true);

        LearningPointsGrantListener listener = new LearningPointsGrantListener(mapper, creditPoints, rabbitTemplate, retry);
        listener.setDeletedUsers(deletedUsers);

        byte[] body = mapper.writeValueAsBytes(new PointsGrantRequested(
                UUID.randomUUID(), userId, 10, "STREAK_MILESTONE", "ref-1"));
        listener.onPointsGrantRequested(new Message(body, new MessageProperties()));

        verify(deletedUsers).isDeleted(userId);
        verifyNoInteractions(creditPoints, rabbitTemplate, retry);
    }
}
