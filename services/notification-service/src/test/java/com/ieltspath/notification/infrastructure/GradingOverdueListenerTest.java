package com.ieltspath.notification.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ieltspath.events.assessment.GradingOverdueV1;
import com.ieltspath.events.messaging.MessageRetry;
import com.ieltspath.notification.application.RecordGradingOverdueUseCase;
import com.ieltspath.notification.domain.vo.Notification;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.dao.QueryTimeoutException;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** How the consumer sorts what it receives: store and push, retry a temporary failure, refuse a bad message. */
class GradingOverdueListenerTest {
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    private final RecordGradingOverdueUseCase record = mock(RecordGradingOverdueUseCase.class);
    private final NoticePushBridge socket = mock(NoticePushBridge.class);
    private final RabbitTemplate rabbit = mock(RabbitTemplate.class);
    private final MessageRetry retry = new MessageRetry(rabbit, GradingOverdueMessagingConfig.RETRY_QUEUE, 2);
    private final GradingOverdueListener listener = new GradingOverdueListener(mapper, record, socket, retry);

    private final UUID learner = UUID.randomUUID();
    private final UUID job = UUID.randomUUID();
    private final GradingOverdueV1 event = new GradingOverdueV1(UUID.randomUUID(), GradingOverdueV1.EVENT_TYPE,
            Instant.parse("2026-10-23T09:10:00Z"), learner, UUID.randomUUID(), job, "WRITING",
            Instant.parse("2026-10-23T09:00:00Z"));

    private Message delivery(byte[] body) {
        return MessageBuilder.withBody(body).setMessageId(event.eventId().toString())
                .setReceivedRoutingKey(GradingOverdueV1.EVENT_TYPE).build();
    }

    private Message delivery() throws Exception {
        return delivery(mapper.writeValueAsBytes(event));
    }

    private Notification stored() {
        return new Notification(event.eventId(), event.examinerId(), learner, event.eventType(), null, null, job,
                event.occurredAt(), null);
    }

    @Test
    void aStoredNoticeIsPushedToTheLearner() throws Exception {
        when(record.execute(event, GradingOverdueV1.EVENT_TYPE, event.eventId().toString()))
                .thenReturn(Optional.of(stored()));

        listener.onGradingOverdue(delivery());

        verify(socket).pushToUser(eq(learner), any());
    }

    @Test
    void aDuplicateIsNotPushedAgain() throws Exception {
        when(record.execute(any(), anyString(), anyString())).thenReturn(Optional.empty());

        listener.onGradingOverdue(delivery());

        verifyNoInteractions(socket);
    }

    @Test
    void aStoredNoticeIsPushedOnceAndNotRetried() throws Exception {
        when(record.execute(any(), anyString(), anyString())).thenReturn(Optional.of(stored()));

        listener.onGradingOverdue(delivery());

        verify(socket).pushToUser(eq(learner), any());
        verify(rabbit, never()).send(anyString(), anyString(), any(Message.class));
    }

    @Test
    void aTemporaryFailureIsParkedOnTheRetryQueueAndRetriedWithTheSameEvent() throws Exception {
        when(record.execute(any(), anyString(), anyString())).thenThrow(new QueryTimeoutException("database busy"))
                .thenReturn(Optional.of(stored()));
        Message message = delivery();

        listener.onGradingOverdue(message);
        verify(rabbit).send(eq(""), eq(GradingOverdueMessagingConfig.RETRY_QUEUE), eq(message));
        assertEquals(1, MessageRetry.retries(message));

        listener.onGradingOverdue(message);
        verify(socket).pushToUser(eq(learner), any());
    }

    @Test
    void aNoticeThatKeepsFailingIsDeadLetteredAfterTheRetries() throws Exception {
        when(record.execute(any(), anyString(), anyString())).thenThrow(new QueryTimeoutException("database busy"));
        Message message = delivery();
        listener.onGradingOverdue(message);
        listener.onGradingOverdue(message);

        assertThrows(AmqpRejectAndDontRequeueException.class, () -> listener.onGradingOverdue(message));
    }

    @Test
    void anUnreadableMessageAndAContractBreakAreDeadLetteredWithoutRetry() throws Exception {
        assertThrows(AmqpRejectAndDontRequeueException.class,
                () -> listener.onGradingOverdue(delivery("not json".getBytes(StandardCharsets.UTF_8))));
        when(record.execute(any(), anyString(), anyString())).thenThrow(new IllegalArgumentException("broken"));
        assertThrows(AmqpRejectAndDontRequeueException.class, () -> listener.onGradingOverdue(delivery()));
        verifyNoInteractions(rabbit, socket);
    }
}
