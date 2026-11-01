package com.ieltspath.access.infrastructure.messaging;

import com.ieltspath.access.domain.entity.OutboxEvent;
import com.ieltspath.commonobservability.Correlation;
import com.ieltspath.events.access.AccessMessaging;
import com.ieltspath.events.access.SubscriptionGrantedV1;
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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Publishes with publisher confirms and mandatory routing: an event counts as published only when the broker confirmed it
 * and routed it to at least one queue. Anything else leaves the outbox row for the next attempt.
 */
@Component
public class RabbitAccessEventPublisher implements AccessEventPublisher {
    private final RabbitTemplate rabbitTemplate;
    private final Duration confirmTimeout;

    public RabbitAccessEventPublisher(RabbitTemplate rabbitTemplate,
                                      @Value("${access.outbox.relay.confirm-timeout:PT5S}") Duration confirmTimeout) {
        this.rabbitTemplate = rabbitTemplate;
        this.confirmTimeout = confirmTimeout;
    }

    @Override
    public void publish(OutboxEvent event) {
        if (!OutboxEvent.RELAYED_EVENT_TYPES.contains(event.getEventType())) {
            throw new IllegalStateException("Event type is not relayed: " + event.getEventType());
        }
        var messageBuilder = MessageBuilder.withBody(event.getPayload().getBytes(StandardCharsets.UTF_8))
                .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                .setContentEncoding(StandardCharsets.UTF_8.name())
                .setMessageId(event.getId().toString())
                .setType(AccessMessaging.SUBSCRIPTION_GRANTED_ROUTING_KEY)
                .setTimestamp(Date.from(event.getCreatedAt()))
                .setDeliveryMode(MessageDeliveryMode.PERSISTENT)
                .setHeader("aggregate_id", event.getAggregateId().toString());
        if (event.getCorrelationId() != null) messageBuilder.setHeader(Correlation.HEADER, event.getCorrelationId());
        Message message = messageBuilder.build();
        CorrelationData correlation = new CorrelationData(event.getId().toString());
        rabbitTemplate.send(AccessMessaging.EXCHANGE, AccessMessaging.SUBSCRIPTION_GRANTED_ROUTING_KEY, message, correlation);
        CorrelationData.Confirm confirm;
        try {
            confirm = correlation.getFuture().get(confirmTimeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for broker confirm", exception);
        } catch (ExecutionException | TimeoutException exception) {
            throw new IllegalStateException("Broker did not confirm the event", exception);
        }
        if (!confirm.isAck()) {
            throw new IllegalStateException("Broker rejected the event: " + confirm.getReason());
        }
        if (correlation.getReturned() != null) {
            throw new IllegalStateException("Event was not routed to any queue: "
                    + correlation.getReturned().getReplyText());
        }
    }
}
