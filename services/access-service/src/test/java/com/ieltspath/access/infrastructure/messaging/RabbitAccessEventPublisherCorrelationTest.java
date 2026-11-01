package com.ieltspath.access.infrastructure.messaging;

import com.ieltspath.access.domain.entity.OutboxEvent;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import org.mockito.ArgumentCaptor;

class RabbitAccessEventPublisherCorrelationTest {
    private final RabbitTemplate rabbit = mock(RabbitTemplate.class);
    private final RabbitAccessEventPublisher publisher = new RabbitAccessEventPublisher(rabbit, Duration.ofSeconds(1));

    private void brokerAcknowledges() {
        doAnswer(invocation -> {
            CorrelationData correlation = invocation.getArgument(3);
            correlation.getFuture().complete(new CorrelationData.Confirm(true, null));
            return null;
        }).when(rabbit).send(anyString(), anyString(), any(Message.class), any(CorrelationData.class));
    }

    private OutboxEvent event(String correlationId) {
        return new OutboxEvent(UUID.randomUUID(), "Subscription", UUID.randomUUID(), "SubscriptionGranted", "{}",
                "PENDING", 0, Instant.now(), null, correlationId);
    }

    @Test
    void sendsHeaderWhenRowHasCorrelationId() {
        brokerAcknowledges();
        ArgumentCaptor<Message> message = ArgumentCaptor.forClass(Message.class);

        publisher.publish(event("req-access"));

        verify(rabbit).send(anyString(), anyString(), message.capture(), any(CorrelationData.class));
        assertEquals("req-access", message.getValue().getMessageProperties().getHeader("x-correlation-id"));
    }

    @Test
    void omitsHeaderWhenRowHasNone() {
        brokerAcknowledges();
        ArgumentCaptor<Message> message = ArgumentCaptor.forClass(Message.class);

        publisher.publish(event(null));

        verify(rabbit).send(anyString(), anyString(), message.capture(), any(CorrelationData.class));
        assertNull(message.getValue().getMessageProperties().getHeader("x-correlation-id"));
    }
}
