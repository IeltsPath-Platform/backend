package com.ieltspath.learning.infrastructure.messaging;

import com.ieltspath.events.points.PointsGrantMessaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.learning.application.port.PointsGrantPublisher;
import com.ieltspath.learning.domain.repository.RewardGrantRepository.PendingGrant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Publishes a grant as JSON on the points exchange; the grant id is also the message id. The call returns only once the
 * broker has confirmed the message and no queue sent it back: a message with no queue bound to the routing key is dropped
 * silently by a plain publish, which would let the relay record a grant nobody received.
 */
@Component
public class RabbitPointsGrantPublisher implements PointsGrantPublisher {
    private static final Logger log = LoggerFactory.getLogger(RabbitPointsGrantPublisher.class);

    private final RabbitTemplate rabbit;
    private final ObjectMapper json;
    private final Duration confirmTimeout;

    @Autowired
    public RabbitPointsGrantPublisher(RabbitTemplate rabbit, ObjectMapper json,
                                      @Value("${learning.rewards.confirm-timeout:PT5S}") Duration confirmTimeout) {
        this.rabbit = rabbit;
        this.json = json;
        this.confirmTimeout = confirmTimeout;
        // The template links a returned message to the publishing call only when it has a returns callback.
        rabbit.setReturnsCallback(returned -> log.warn("Points grant returned by the broker: messageId={}, replyCode={}, routingKey={}",
                returned.getMessage().getMessageProperties().getMessageId(), returned.getReplyCode(),
                returned.getRoutingKey()));
    }

    @Override
    public void publish(PendingGrant grant) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("grantId", grant.id());
        body.put("userId", grant.userId());
        body.put("amount", grant.amount());
        body.put("reason", grant.reason());
        body.put("referenceId", grant.referenceId());
        Message message;
        try {
            message = MessageBuilder.withBody(json.writeValueAsBytes(body))
                    .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                    .setDeliveryMode(MessageDeliveryMode.PERSISTENT)
                    .setMessageId(grant.id().toString()).build();
        } catch (JsonProcessingException failure) {
            throw new IllegalStateException("Points grant cannot be written", failure);
        }
        CorrelationData correlation = new CorrelationData(grant.id().toString());
        rabbit.send(PointsGrantMessaging.EXCHANGE, PointsGrantMessaging.REQUESTED_ROUTING_KEY, message,
                correlation);
        awaitConfirm(correlation);
        if (correlation.getReturned() != null) {
            throw new GrantNotDeliveredException("No queue is bound for the points grant");
        }
    }

    private void awaitConfirm(CorrelationData correlation) {
        CorrelationData.Confirm confirm;
        try {
            confirm = correlation.getFuture().get(confirmTimeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new GrantNotDeliveredException("Interrupted while waiting for the broker to confirm the points grant");
        } catch (ExecutionException | TimeoutException failure) {
            throw new GrantNotDeliveredException("The broker did not confirm the points grant");
        }
        if (!confirm.isAck()) throw new GrantNotDeliveredException("The broker refused the points grant");
    }

    /** The broker did not take the grant; it was not delivered and the relay keeps it due. */
    public static class GrantNotDeliveredException extends RuntimeException {
        public GrantNotDeliveredException(String message) {
            super(message);
        }
    }
}
