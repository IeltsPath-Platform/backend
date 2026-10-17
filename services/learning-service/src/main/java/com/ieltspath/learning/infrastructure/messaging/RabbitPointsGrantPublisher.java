package com.ieltspath.learning.infrastructure.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.learning.application.port.PointsGrantPublisher;
import com.ieltspath.learning.domain.repository.RewardGrantRepository.PendingGrant;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/** Publishes a grant as JSON on the points exchange; the grant id is also the message id. */
@Component
public class RabbitPointsGrantPublisher implements PointsGrantPublisher {
    private final RabbitTemplate rabbit;
    private final ObjectMapper json;

    public RabbitPointsGrantPublisher(RabbitTemplate rabbit, ObjectMapper json) {
        this.rabbit = rabbit;
        this.json = json;
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
        rabbit.send(RewardGrantMessagingConfig.EXCHANGE, RewardGrantMessagingConfig.REQUESTED_ROUTING_KEY, message);
    }
}
