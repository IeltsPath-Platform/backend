package com.ieltspath.notification.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ieltspath.events.assessment.GradingRequestedV1;
import com.ieltspath.events.messaging.MessageRetry;
import com.ieltspath.notification.application.RecordGradingRequestedUseCase;
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

/** How the consumer sorts what it receives: store and push to the examiner, retry a temporary failure, refuse a bad message. */
class GradingRequestedListenerTest {
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    private final RecordGradingRequestedUseCase record = mock(RecordGradingRequestedUseCase.class);
    private final NoticePushBridge socket = mock(NoticePushBridge.class);
    private final RabbitTemplate rabbit = mock(RabbitTemplate.class);
    private final MessageRetry retry = new MessageRetry(rabbit, GradingRequestedMessagingConfig.RETRY_QUEUE, 2);
    private final GradingRequestedListener listener = new GradingRequestedListener(mapper, record, socket, retry);

    private final UUID examiner = UUID.randomUUID();
    private final UUID job = UUID.randomUUID();
    private final GradingRequestedV1 event = new GradingRequestedV1(UUID.randomUUID(), GradingRequestedV1.EVENT_TYPE,
            Instant.parse("2026-10-23T21:10:00Z"), examiner, job, "WRITING", Instant.parse("2026-10-24T09:00:00Z"));

    private Message delivery(byte[] body) {
        return MessageBuilder.withBody(body).setMessageId(event.eventId().toString())
                .setReceivedRoutingKey(GradingRequestedV1.EVENT_TYPE).build();
    }

    private Message delivery() throws Exception {
        return delivery(mapper.writeValueAsBytes(event));
    }

    private Notification stored() {
        return new Notification(event.eventId(), examiner, examiner, event.eventType(), null, null, job,
                event.occurredAt(), null);
    }

    @Test
    void aStoredNoticeIsPushedToTheExaminer() throws Exception {
        when(record.execute(event, GradingRequestedV1.EVENT_TYPE, event.eventId().toString()))
                .thenReturn(Optional.of(stored()));

        listener.onGradingRequested(delivery());

        verify(socket).pushToUser(eq(examiner), any());
    }

    @Test
    void aDuplicateIsNotPushedAgain() throws Exception {
        when(record.execute(any(), anyString(), anyString())).thenReturn(Optional.empty());

        listener.onGradingRequested(delivery());

        verifyNoInteractions(socket);
    }

    @Test
    void aTemporaryFailureIsRetriedAndAKeepsFailingReminderIsDeadLettered() throws Exception {
        when(record.execute(any(), anyString(), anyString())).thenThrow(new QueryTimeoutException("database busy"));
        Message message = delivery();

        listener.onGradingRequested(message);
        verify(rabbit).send(eq(""), eq(GradingRequestedMessagingConfig.RETRY_QUEUE), eq(message));
        assertEquals(1, MessageRetry.retries(message));
        listener.onGradingRequested(message);

        assertThrows(AmqpRejectAndDontRequeueException.class, () -> listener.onGradingRequested(message));
    }

    @Test
    void anUnreadableMessageAndAContractBreakAreDeadLetteredWithoutRetry() throws Exception {
        assertThrows(AmqpRejectAndDontRequeueException.class,
                () -> listener.onGradingRequested(delivery("not json".getBytes(StandardCharsets.UTF_8))));
        when(record.execute(any(), anyString(), anyString())).thenThrow(new IllegalArgumentException("broken"));
        assertThrows(AmqpRejectAndDontRequeueException.class, () -> listener.onGradingRequested(delivery()));
        verifyNoInteractions(rabbit, socket);
    }
}
