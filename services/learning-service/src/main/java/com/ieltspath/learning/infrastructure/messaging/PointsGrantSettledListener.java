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

import java.io.IOException;
import java.util.UUID;

/**
 * Reads Access's answer to a points grant and records it. A message that cannot be read is refused and dead-lettered: it
 * would never become readable. A failure to record is treated as temporary: the message goes to the retry queue and,
 * after the allowed retries, to the dead-letter queue. Recording is a single idempotent update, so handling a message
 * twice is harmless. Contents are not logged.
 */
@Component
public class PointsGrantSettledListener {
    private static final Logger log = LoggerFactory.getLogger(PointsGrantSettledListener.class);

    private final RewardGrantService grants;
    private final ObjectMapper json;
    private final MessageRetry retry;

    public PointsGrantSettledListener(RewardGrantService grants, ObjectMapper json, MessageRetry retry) {
        this.grants = grants;
        this.json = json;
        this.retry = retry;
    }

    @RabbitListener(queues = RewardGrantMessagingConfig.SETTLED_QUEUE,
            containerFactory = RewardGrantMessagingConfig.CONTAINER_FACTORY,
            autoStartup = "${learning.messaging.consumer-enabled:true}")
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
            log.warn("Unreadable points grant answer dead-lettered");
            throw new AmqpRejectAndDontRequeueException("unreadable points grant answer", unreadable);
        }
        try {
            grants.settle(grantId, status, error);
        } catch (RuntimeException temporary) {
            log.warn("Points grant answer will be retried: errorType={}", temporary.getClass().getSimpleName());
            retry.retryLater(message, temporary);
        }
    }
}
