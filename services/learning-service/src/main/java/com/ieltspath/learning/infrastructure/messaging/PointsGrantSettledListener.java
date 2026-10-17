package com.ieltspath.learning.infrastructure.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.learning.application.service.RewardGrantService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.UUID;

/**
 * Reads Access's answer to a points grant and records it. A message that cannot be read is dropped, not requeued: it
 * would never become readable. A failure to record is thrown so the broker delivers it again. Contents are not logged.
 */
@Component
public class PointsGrantSettledListener {
    private static final Logger log = LoggerFactory.getLogger(PointsGrantSettledListener.class);

    private final RewardGrantService grants;
    private final ObjectMapper json;

    public PointsGrantSettledListener(RewardGrantService grants, ObjectMapper json) {
        this.grants = grants;
        this.json = json;
    }

    @RabbitListener(queues = RewardGrantMessagingConfig.SETTLED_QUEUE,
            autoStartup = "${learning.messaging.consumer-enabled:true}")
    @Transactional
    public void onSettled(Message message) {
        UUID grantId;
        String status;
        String error;
        try {
            JsonNode root = json.readTree(message.getBody());
            grantId = UUID.fromString(root.path("grantId").asText(""));
            status = root.path("status").asText(null);
            error = root.path("error").isNull() ? null : root.path("error").asText(null);
        } catch (IOException | IllegalArgumentException unreadable) {
            log.warn("Unreadable points grant answer dropped");
            throw new AmqpRejectAndDontRequeueException("unreadable points grant answer", unreadable);
        }
        grants.settle(grantId, status, error);
    }
}
