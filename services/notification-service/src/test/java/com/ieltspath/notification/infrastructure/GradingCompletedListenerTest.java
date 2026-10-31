package com.ieltspath.notification.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ieltspath.events.assessment.GradingCompletedV1;
import com.ieltspath.events.messaging.MessageRetry;
import com.ieltspath.notification.application.RecordGradingCompletedUseCase;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** How the consumer sorts what it receives: store and push to the learner, retry a temporary failure, refuse a bad message. */
class GradingCompletedListenerTest {
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    private final RecordGradingCompletedUseCase record = mock(RecordGradingCompletedUseCase.class);
    private final NoticePushBridge socket = mock(NoticePushBridge.class);
    private final RabbitTemplate rabbit = mock(RabbitTemplate.class);
    private final MessageRetry retry = new MessageRetry(rabbit, GradingCompletedMessagingConfig.RETRY_QUEUE, 2);
    private final GradingCompletedListener listener = new GradingCompletedListener(mapper, record, socket, retry);

    private final UUID learner = UUID.randomUUID();
    private final UUID job = UUID.randomUUID();
    private final GradingCompletedV1 event = new GradingCompletedV1(UUID.randomUUID(), GradingCompletedV1.EVENT_TYPE,
            Instant.parse("2026-10-26T13:00:00Z"), learner, job, UUID.randomUUID(), "WRITING");

    private Message delivery(byte[] body) {
        return MessageBuilder.withBody(body).setMessageId(event.eventId().toString())
                .setReceivedRoutingKey(GradingCompletedV1.EVENT_TYPE).build();
    }

    private Message delivery() throws Exception {
        return delivery(mapper.writeValueAsBytes(event));
    }

    private Notification stored() {
        return new Notification(event.eventId(), learner, learner, event.eventType(), null, null, job,
                event.occurredAt(), null);
    }

    @Test
    void aStoredNoticeIsPushedToTheLearner() throws Exception {
        when(record.execute(event, GradingCompletedV1.EVENT_TYPE, event.eventId().toString()))
                .thenReturn(Optional.of(stored()));

        listener.onGradingCompleted(delivery());

        verify(socket).pushToUser(eq(learner), any());
    }

    @Test
    void aDuplicateIsNotPushedAgain() throws Exception {
        when(record.execute(any(), anyString(), anyString())).thenReturn(Optional.empty());

        listener.onGradingCompleted(delivery());

        verifyNoInteractions(socket);
    }

    @Test
    void aTemporaryFailureIsRetriedAndAKeepsFailingReminderIsDeadLettered() throws Exception {
        when(record.execute(any(), anyString(), anyString())).thenThrow(new QueryTimeoutException("database busy"));
        Message message = delivery();

        listener.onGradingCompleted(message);
        verify(rabbit).send(eq(""), eq(GradingCompletedMessagingConfig.RETRY_QUEUE), eq(message));
        assertEquals(1, MessageRetry.retries(message));
        listener.onGradingCompleted(message);

        assertThrows(AmqpRejectAndDontRequeueException.class, () -> listener.onGradingCompleted(message));
    }

    @Test
    void anUnreadableMessageAndAContractBreakAreDeadLetteredWithoutRetry() throws Exception {
        assertThrows(AmqpRejectAndDontRequeueException.class,
                () -> listener.onGradingCompleted(delivery("not json".getBytes(StandardCharsets.UTF_8))));
        when(record.execute(any(), anyString(), anyString())).thenThrow(new IllegalArgumentException("broken"));
        assertThrows(AmqpRejectAndDontRequeueException.class, () -> listener.onGradingCompleted(delivery()));
        verifyNoInteractions(rabbit, socket);
    }
}
