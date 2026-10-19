package com.ieltspath.events.messaging;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class MessageRetryTest {
    private final RabbitTemplate rabbit = mock(RabbitTemplate.class);
    private final MessageRetry retry = new MessageRetry(rabbit, "grant.retry", 2);

    private static Message message() {
        return MessageBuilder.withBody("{}".getBytes(StandardCharsets.UTF_8)).build();
    }

    @Test
    void aFirstFailureGoesToTheRetryQueueWithTheCountRaised() {
        Message message = message();

        retry.retryLater(message, new IllegalStateException("database down"));

        ArgumentCaptor<Message> sent = ArgumentCaptor.forClass(Message.class);
        verify(rabbit).send(eq(""), eq("grant.retry"), sent.capture());
        assertSame(message, sent.getValue());
        assertEquals(1, MessageRetry.retries(sent.getValue()));
        assertEquals(1, (Integer) sent.getValue().getMessageProperties().getHeader("x-retry-count"));
    }

    @Test
    void aRetriedMessageKeepsItsIdentityAndBodySoTheConsumerStaysIdempotent() {
        Message message = MessageBuilder.withBody("{\"grantId\":\"g-1\"}".getBytes(StandardCharsets.UTF_8))
                .setMessageId("message-1").build();

        retry.retryLater(message, new IllegalStateException());
        retry.retryLater(message, new IllegalStateException());

        ArgumentCaptor<Message> sent = ArgumentCaptor.forClass(Message.class);
        verify(rabbit, times(2)).send(eq(""), eq("grant.retry"), sent.capture());
        for (Message resent : sent.getAllValues()) {
            assertEquals("message-1", resent.getMessageProperties().getMessageId());
            assertEquals("{\"grantId\":\"g-1\"}", new String(resent.getBody(), StandardCharsets.UTF_8));
        }
    }

    @Test
    void theCountKeepsGrowingUntilTheRetriesAreUsedUp() {
        Message message = message();
        retry.retryLater(message, new IllegalStateException());
        retry.retryLater(message, new IllegalStateException());
        assertEquals(2, MessageRetry.retries(message));

        assertThrows(AmqpRejectAndDontRequeueException.class,
                () -> retry.retryLater(message, new IllegalStateException()));
        verify(rabbit, times(2)).send(any(String.class), any(String.class), any(Message.class));
    }

    @Test
    void aMessageWithNoCountIsAtItsFirstAttempt() {
        assertEquals(0, MessageRetry.retries(message()));
        verify(rabbit, never()).send(any(String.class), any(String.class), any(Message.class));
    }
}
