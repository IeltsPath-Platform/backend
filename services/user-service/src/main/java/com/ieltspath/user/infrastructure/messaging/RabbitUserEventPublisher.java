package com.ieltspath.user.infrastructure.messaging;

import com.ieltspath.commonobservability.Correlation;
import com.ieltspath.events.user.PasswordResetRequestedV1;
import com.ieltspath.events.user.UserMessaging;
import com.ieltspath.user.domain.aggregate.OutboxEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.time.Instant;
import java.util.Date;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
public class RabbitUserEventPublisher implements UserEventPublisher {
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper mapper;
    private final Duration confirmTimeout;

    public RabbitUserEventPublisher(RabbitTemplate rabbitTemplate, ObjectMapper mapper,
                                   @Value("${user.outbox.relay.confirm-timeout:PT5S}") Duration confirmTimeout) {
        this.rabbitTemplate = rabbitTemplate;
        this.mapper = mapper;
        this.confirmTimeout = confirmTimeout;
    }

    @Override
    public void publish(OutboxEvent event) {
        if (!PasswordResetRequestedV1.EVENT_TYPE.equals(event.eventType())) {
            throw new IllegalStateException("Unsupported user event type");
        }
        try {
            PasswordResetRequestedV1 payload = mapper.readValue(event.payload(), PasswordResetRequestedV1.class);
            long ttlMillis = Math.max(0, Duration.between(Instant.now(), payload.expiresAt()).toMillis());
            var builder = MessageBuilder.withBody(event.payload().getBytes(StandardCharsets.UTF_8))
                    .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                    .setContentEncoding(StandardCharsets.UTF_8.name())
                    .setMessageId(event.id().toString())
                    .setType(event.eventType())
                    .setTimestamp(Date.from(event.createdAt()))
                    .setDeliveryMode(MessageDeliveryMode.PERSISTENT)
                    .setExpiration(Long.toString(ttlMillis));
            if (event.correlationId() != null) builder.setHeader(Correlation.HEADER, event.correlationId());
            Message message = builder.build();
            CorrelationData correlation = new CorrelationData(event.id().toString());
            rabbitTemplate.send(UserMessaging.EXCHANGE, event.eventType(), message, correlation);
            CorrelationData.Confirm confirm = correlation.getFuture().get(confirmTimeout.toMillis(), TimeUnit.MILLISECONDS);
            if (!confirm.isAck() || correlation.getReturned() != null) {
                throw new IllegalStateException("Broker did not confirm a routed event");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted waiting for broker confirm", exception);
        } catch (ExecutionException | TimeoutException | java.io.IOException exception) {
            throw new IllegalStateException("Broker did not confirm the event", exception);
        }
    }
}
