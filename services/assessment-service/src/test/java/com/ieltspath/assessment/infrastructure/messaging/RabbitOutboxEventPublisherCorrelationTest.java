package com.ieltspath.assessment.infrastructure.messaging;

import com.ieltspath.assessment.domain.entity.OutboxEvent;
import com.ieltspath.events.assessment.AssessmentCompletedV2;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.Instant;
import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import org.mockito.ArgumentCaptor;

class RabbitOutboxEventPublisherCorrelationTest {
    private final RabbitTemplate rabbit = mock(RabbitTemplate.class);
    private final RabbitOutboxEventPublisher publisher = new RabbitOutboxEventPublisher(rabbit, Duration.ofSeconds(1));

    private void brokerAcknowledges() {
        doAnswer(invocation -> {
            CorrelationData correlation = invocation.getArgument(3);
            correlation.getFuture().complete(new CorrelationData.Confirm(true, null));
            return null;
        }).when(rabbit).send(anyString(), anyString(), any(Message.class), any(CorrelationData.class));
    }

    private OutboxEvent event(String correlationId) {
        return new OutboxEvent(UUID.randomUUID(), "AssessmentResult", UUID.randomUUID().toString(),
                AssessmentCompletedV2.EVENT_TYPE, "{}", Instant.now(), null, 0, null, correlationId);
    }

    @Test
    void sendsHeaderWhenRowHasCorrelationId() {
        brokerAcknowledges();
        ArgumentCaptor<Message> message = ArgumentCaptor.forClass(Message.class);

        publisher.publish(event("req-assessment"));

        verify(rabbit).send(anyString(), anyString(), message.capture(), any(CorrelationData.class));
        assertEquals("req-assessment", message.getValue().getMessageProperties().getHeader("x-correlation-id"));
    }

    @Test
    void omitsHeaderWhenRowHasNone() {
        brokerAcknowledges();
        ArgumentCaptor<Message> message = ArgumentCaptor.forClass(Message.class);

        publisher.publish(event(null));

        verify(rabbit).send(anyString(), anyString(), message.capture(), any(CorrelationData.class));
        assertNull(message.getValue().getMessageProperties().getHeader("x-correlation-id"));
    }

    @Test
    void routesTheExaminersGradeToLearningOnTheAssessmentExchange() {
        brokerAcknowledges();
        OutboxEvent graded = new OutboxEvent(UUID.randomUUID(), "HumanReviewSubmission", UUID.randomUUID().toString(),
                com.ieltspath.events.assessment.HumanReviewCompletedV1.EVENT_TYPE, "{}", Instant.now(), null, 0, null, null);

        publisher.publish(graded);

        verify(rabbit).send(org.mockito.ArgumentMatchers.eq(com.ieltspath.events.assessment.AssessmentMessaging.EXCHANGE),
                org.mockito.ArgumentMatchers.eq(com.ieltspath.events.assessment.AssessmentMessaging.HUMAN_REVIEW_COMPLETED_ROUTING_KEY),
                any(Message.class), any(CorrelationData.class));
    }
}
