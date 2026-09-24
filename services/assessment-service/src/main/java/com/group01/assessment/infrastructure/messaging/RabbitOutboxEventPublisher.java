package com.group01.assessment.infrastructure.messaging;

import com.group01.assessment.application.event.AssessmentCompletedV2;
import com.group01.assessment.domain.entity.OutboxEvent;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Publishes with publisher confirms and mandatory routing: an event counts as published only when the broker
 * confirmed it and routed it to at least one queue. Anything else leaves the outbox row for the next attempt.
 */
@Component
public class RabbitOutboxEventPublisher implements OutboxEventPublisher {
    static final Map<String, String> ROUTING_KEYS = Map.of(
            AssessmentCompletedV2.EVENT_TYPE, "assessment.completed.v2");

    private final RabbitTemplate rabbitTemplate;
    private final String exchange;
    private final Duration confirmTimeout;

    public RabbitOutboxEventPublisher(RabbitTemplate rabbitTemplate,
                                      @Value("${assessment.messaging.exchange:assessment.events}") String exchange,
                                      @Value("${assessment.outbox.relay.confirm-timeout:PT5S}") Duration confirmTimeout) {
        this.rabbitTemplate = rabbitTemplate;
        this.exchange = exchange;
        this.confirmTimeout = confirmTimeout;
    }

    @Override
    public void publish(OutboxEvent event) {
        String routingKey = ROUTING_KEYS.get(event.eventType());
        if (routingKey == null) {
            throw new PublishFailedException("No routing key is configured for event type " + event.eventType());
        }
        Message message = MessageBuilder.withBody(event.payload().getBytes(StandardCharsets.UTF_8))
                .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                .setContentEncoding(StandardCharsets.UTF_8.name())
                .setMessageId(event.id().toString())
                .setType(event.eventType())
                .setTimestamp(Date.from(event.createdAt()))
                .setDeliveryMode(MessageDeliveryMode.PERSISTENT)
                .setHeader("event_type", event.eventType())
                .setHeader("aggregate_id", event.aggregateId())
                .build();
        CorrelationData correlation = new CorrelationData(event.id().toString());
        rabbitTemplate.send(exchange, routingKey, message, correlation);
        CorrelationData.Confirm confirm;
        try {
            confirm = correlation.getFuture().get(confirmTimeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new PublishFailedException("Interrupted while waiting for broker confirm", exception);
        } catch (ExecutionException | TimeoutException exception) {
            throw new PublishFailedException("Broker did not confirm the event", exception);
        }
        if (!confirm.isAck()) {
            throw new PublishFailedException("Broker rejected the event: " + confirm.getReason());
        }
        if (correlation.getReturned() != null) {
            throw new PublishFailedException("Event was not routed to any queue: "
                    + correlation.getReturned().getReplyText());
        }
    }
}
