package com.ieltspath.access.infrastructure.messaging;

import com.ieltspath.events.messaging.MessageRetry;

import com.ieltspath.events.points.PointsGrantMessaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.access.application.result.PointCreditResult;
import com.ieltspath.access.application.usecase.CreditPointsUseCase;
import com.ieltspath.access.domain.vo.LearningRewardReason;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.QueryTimeoutException;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** How the consumer sorts a failure: a temporary one goes to the retry queue, a refusal is dead-lettered. */
class LearningPointsGrantListenerFailureTest {
    private final CreditPointsUseCase credits = mock(CreditPointsUseCase.class);
    private final RabbitTemplate rabbit = mock(RabbitTemplate.class);
    private final MessageRetry retry = new MessageRetry(rabbit, LearningPointsMessagingConfig.GRANT_RETRY_QUEUE, 2);
    private final LearningPointsGrantListener listener =
            new LearningPointsGrantListener(new ObjectMapper(), credits, rabbit, retry);

    private static Message grant(UUID grantId) {
        String body = "{\"grantId\":\"" + grantId + "\",\"userId\":\"" + UUID.randomUUID()
                + "\",\"amount\":3,\"reason\":\"DAILY_QUEST\",\"referenceId\":\"quest:1\"}";
        return MessageBuilder.withBody(body.getBytes(StandardCharsets.UTF_8)).build();
    }

    private void creditFailsWith(RuntimeException failure) {
        when(credits.executeLearningReward(any(), any(), anyInt(), any(LearningRewardReason.class), anyString()))
                .thenThrow(failure);
    }

    @Test
    void aTemporaryFailureIsParkedOnTheRetryQueueAndTheDeliveryIsAcknowledged() {
        creditFailsWith(new QueryTimeoutException("database busy"));
        Message message = grant(UUID.randomUUID());

        listener.onPointsGrantRequested(message);

        verify(rabbit).send(eq(""), eq(LearningPointsMessagingConfig.GRANT_RETRY_QUEUE), eq(message));
        assertEquals(1, MessageRetry.retries(message));
        verify(rabbit, never()).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    @Test
    void theRetriedDeliveryCreditsTheSameGrantIdSoNothingIsPaidTwice() {
        UUID grantId = UUID.randomUUID();
        when(credits.executeLearningReward(any(), any(), anyInt(), any(LearningRewardReason.class), anyString()))
                .thenThrow(new QueryTimeoutException("database busy"))
                .thenReturn(new PointCreditResult(3, false, 3, UUID.randomUUID()));
        Message message = grant(grantId);

        listener.onPointsGrantRequested(message);
        listener.onPointsGrantRequested(message);

        verify(credits, org.mockito.Mockito.times(2)).executeLearningReward(eq(grantId), any(), anyInt(),
                any(LearningRewardReason.class), anyString());
        verify(rabbit).convertAndSend(eq(PointsGrantMessaging.EXCHANGE),
                eq(PointsGrantMessaging.SETTLED_ROUTING_KEY), any(Object.class));
    }

    @Test
    void aGrantThatKeepsFailingIsDeadLetteredAfterTheRetries() {
        creditFailsWith(new QueryTimeoutException("database busy"));
        Message message = grant(UUID.randomUUID());
        listener.onPointsGrantRequested(message);
        listener.onPointsGrantRequested(message);

        assertThrows(AmqpRejectAndDontRequeueException.class, () -> listener.onPointsGrantRequested(message));
    }

    @Test
    void aConstraintViolationIsDeadLetteredWithoutRetry() {
        creditFailsWith(new DataIntegrityViolationException("duplicate key"));

        assertThrows(AmqpRejectAndDontRequeueException.class,
                () -> listener.onPointsGrantRequested(grant(UUID.randomUUID())));
        verifyNoInteractions(rabbit);
    }

    @Test
    void aSettlementThatCannotBePublishedIsRetriedBecauseCreditingIsIdempotent() {
        when(credits.executeLearningReward(any(), any(), anyInt(), any(LearningRewardReason.class), anyString()))
                .thenReturn(new PointCreditResult(3, false, 3, UUID.randomUUID()));
        doThrow(new AmqpConnectException(new RuntimeException("down")))
                .when(rabbit).convertAndSend(anyString(), anyString(), any(Object.class));
        Message message = grant(UUID.randomUUID());

        listener.onPointsGrantRequested(message);

        verify(rabbit).send(eq(""), eq(LearningPointsMessagingConfig.GRANT_RETRY_QUEUE), eq(message));
    }
}
