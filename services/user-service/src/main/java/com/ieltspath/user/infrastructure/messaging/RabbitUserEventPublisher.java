package com.ieltspath.user.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.commonobservability.Correlation;
import com.ieltspath.events.user.EmailVerificationRequestedV1;
import com.ieltspath.events.user.GoogleAccountLinkedV1;
import com.ieltspath.events.user.PasswordChangedV1;
import com.ieltspath.events.user.PasswordResetRequestedV1;
import com.ieltspath.events.user.UserEmailVerificationStateV1;
import com.ieltspath.events.user.UserMessaging;
import com.ieltspath.user.domain.aggregate.OutboxEvent;
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
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
public class RabbitUserEventPublisher implements UserEventPublisher {
    private static final Set<String> SUPPORTED_EVENT_TYPES = Set.of(
            PasswordResetRequestedV1.EVENT_TYPE, PasswordChangedV1.EVENT_TYPE, GoogleAccountLinkedV1.EVENT_TYPE,
            EmailVerificationRequestedV1.EVENT_TYPE, UserEmailVerificationStateV1.EVENT_TYPE);

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
        if (!SUPPORTED_EVENT_TYPES.contains(event.eventType())) {
            throw new IllegalStateException("Unsupported user event type");
        }
        try {
            var builder = MessageBuilder.withBody(event.payload().getBytes(StandardCharsets.UTF_8))
                    .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                    .setContentEncoding(StandardCharsets.UTF_8.name())
                    .setMessageId(event.id().toString())
                    .setType(event.eventType())
                    .setTimestamp(Date.from(event.createdAt()))
                    .setDeliveryMode(MessageDeliveryMode.PERSISTENT);
            // Only a link with a token expires (reset, verification); the other notices stay valid until delivered.
            Instant linkExpiresAt = linkExpiry(event);
            if (linkExpiresAt != null) {
                long ttlMillis = Math.max(0, Duration.between(Instant.now(), linkExpiresAt).toMillis());
                builder.setExpiration(Long.toString(ttlMillis));
            }
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

    private Instant linkExpiry(OutboxEvent event) throws java.io.IOException {
        if (PasswordResetRequestedV1.EVENT_TYPE.equals(event.eventType())) {
            return mapper.readValue(event.payload(), PasswordResetRequestedV1.class).expiresAt();
        }
        if (EmailVerificationRequestedV1.EVENT_TYPE.equals(event.eventType())) {
            return mapper.readValue(event.payload(), EmailVerificationRequestedV1.class).expiresAt();
        }
        return null;
    }
}
