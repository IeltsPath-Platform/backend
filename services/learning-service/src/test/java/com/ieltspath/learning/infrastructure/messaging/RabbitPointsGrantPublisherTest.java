package com.ieltspath.learning.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.learning.domain.repository.RewardGrantRepository.PendingGrant;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.core.ReturnedMessage;

import java.time.Duration;
import java.util.UUID;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

/** A grant counts as sent only when the broker confirms it and no queue returned it. */
class RabbitPointsGrantPublisherTest {
    private final RabbitTemplate rabbit = mock(RabbitTemplate.class);
    private final RabbitPointsGrantPublisher publisher =
            new RabbitPointsGrantPublisher(rabbit, new ObjectMapper(), Duration.ofMillis(200));
    private final PendingGrant grant = new PendingGrant(UUID.randomUUID(), UUID.randomUUID(), 5, "BADGE", "STREAK_7", 0);

    private void brokerAnswers(Consumer<CorrelationData> answer) {
        doAnswer(call -> {
            answer.accept(call.getArgument(3));
            return null;
        }).when(rabbit).send(anyString(), anyString(), any(Message.class), any(CorrelationData.class));
    }

    @Test
    void aConfirmedGrantIsSent() {
        brokerAnswers(correlation -> correlation.getFuture().complete(new CorrelationData.Confirm(true, null)));

        assertDoesNotThrow(() -> publisher.publish(grant));
    }

    @Test
    void aRefusedGrantIsNotCountedAsSent() {
        brokerAnswers(correlation -> correlation.getFuture().complete(new CorrelationData.Confirm(false, "nack")));

        assertThrows(RabbitPointsGrantPublisher.GrantNotDeliveredException.class, () -> publisher.publish(grant));
    }

    @Test
    void aGrantNoQueueTookIsNotCountedAsSent() {
        brokerAnswers(correlation -> {
            Message message = MessageBuilder.withBody(new byte[0]).build();
            correlation.setReturned(new ReturnedMessage(message, 312, "NO_ROUTE", "learning.points",
                    "points.grant.requested"));
            correlation.getFuture().complete(new CorrelationData.Confirm(true, null));
        });

        assertThrows(RabbitPointsGrantPublisher.GrantNotDeliveredException.class, () -> publisher.publish(grant));
    }

    @Test
    void noAnswerFromTheBrokerIsNotCountedAsSent() {
        brokerAnswers(correlation -> { });

        assertThrows(RabbitPointsGrantPublisher.GrantNotDeliveredException.class, () -> publisher.publish(grant));
    }
}
