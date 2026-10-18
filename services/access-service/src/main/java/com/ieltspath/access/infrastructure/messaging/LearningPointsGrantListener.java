package com.ieltspath.access.infrastructure.messaging;

import com.ieltspath.events.points.PointsGrantMessaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.access.application.result.PointCreditResult;
import com.ieltspath.access.application.usecase.CreditPointsUseCase;
import com.ieltspath.access.domain.exception.DuplicateIdempotencyException;
import com.ieltspath.access.domain.vo.LearningRewardReason;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

/**
 * Credits the points Learning asks for and answers on {@code points.grant.settled}. A payload that cannot be read is
 * answered {@code FAILED}. A grant that violates a constraint is refused and dead-lettered. Any other failure is treated
 * as temporary: the message goes to the retry queue and, after the allowed retries, to the dead-letter queue. Crediting is
 * idempotent per grant id, so handling a message twice never pays twice. Grant contents are never logged.
 */
@Component
public class LearningPointsGrantListener {
    private static final Logger log = LoggerFactory.getLogger(LearningPointsGrantListener.class);

    private final ObjectMapper objectMapper;
    private final CreditPointsUseCase creditPoints;
    private final RabbitTemplate rabbitTemplate;
    private final MessageRetry retry;

    public LearningPointsGrantListener(ObjectMapper objectMapper,
                                       CreditPointsUseCase creditPoints,
                                       RabbitTemplate rabbitTemplate,
                                       MessageRetry retry) {
        this.objectMapper = objectMapper;
        this.creditPoints = creditPoints;
        this.rabbitTemplate = rabbitTemplate;
        this.retry = retry;
    }

    @RabbitListener(queues = LearningPointsMessagingConfig.GRANT_QUEUE,
            containerFactory = LearningPointsMessagingConfig.CONTAINER_FACTORY)
    public void onPointsGrantRequested(Message message) {
        try {
            handle(message);
        } catch (AmqpRejectAndDontRequeueException refused) {
            throw refused;
        } catch (RuntimeException temporary) {
            log.warn("Points grant will be retried: errorType={}", temporary.getClass().getSimpleName());
            retry.retryLater(message, temporary);
        }
    }

    private void handle(Message message) {
        GrantRequest request;
        try {
            request = parse(message.getBody());
        } catch (InvalidGrantPayload invalid) {
            settle(PointsGrantSettled.failed(invalid.grantId(), "INVALID_PAYLOAD"));
            return;
        }

        PointCreditResult result;
        try {
            result = creditPoints.executeLearningReward(request.grantId(), request.userId(), request.amount(),
                    request.reason(), request.referenceId());
        } catch (DuplicateIdempotencyException conflict) {
            settle(PointsGrantSettled.failed(request.grantId(), "GRANT_ID_CONFLICT"));
            return;
        } catch (IllegalArgumentException invalid) {
            settle(PointsGrantSettled.failed(request.grantId(), "INVALID_PAYLOAD"));
            return;
        } catch (DataIntegrityViolationException violation) {
            throw new AmqpRejectAndDontRequeueException("Points grant violates a constraint", violation);
        }

        settle(result.capped()
                ? PointsGrantSettled.capped(request.grantId())
                : PointsGrantSettled.granted(request.grantId()));
    }

    private GrantRequest parse(byte[] body) {
        UUID grantId = null;
        try {
            JsonNode root = objectMapper.readTree(body);
            if (root == null || !root.isObject()) throw new InvalidGrantPayload(null);

            try {
                grantId = UUID.fromString(text(root, "grantId"));
            } catch (IllegalArgumentException invalidId) {
                throw new InvalidGrantPayload(null);
            }

            UUID userId;
            try {
                userId = UUID.fromString(text(root, "userId"));
            } catch (IllegalArgumentException invalidId) {
                throw new InvalidGrantPayload(grantId);
            }

            JsonNode amountNode = root.get("amount");
            if (amountNode == null || !amountNode.canConvertToInt()) throw new InvalidGrantPayload(grantId);
            int amount = amountNode.intValue();
            if (amount < 1 || amount > 100) throw new InvalidGrantPayload(grantId);

            LearningRewardReason reason = LearningRewardReason.valueOf(text(root, "reason"));
            String referenceId = text(root, "referenceId");
            if (referenceId.isBlank() || referenceId.length() > 100) throw new InvalidGrantPayload(grantId);

            return new GrantRequest(grantId, userId, amount, reason, referenceId);
        } catch (IOException | IllegalArgumentException malformed) {
            throw new InvalidGrantPayload(grantId);
        }
    }

    private static String text(JsonNode root, String field) {
        JsonNode value = root.get(field);
        if (value == null || !value.isTextual()) throw new IllegalArgumentException("Invalid field " + field);
        return value.textValue();
    }

    private void settle(PointsGrantSettled settled) {
        rabbitTemplate.convertAndSend(PointsGrantMessaging.EXCHANGE,
                PointsGrantMessaging.SETTLED_ROUTING_KEY, settled);
    }

    private record GrantRequest(UUID grantId, UUID userId, int amount,
                                LearningRewardReason reason, String referenceId) {}

    private static final class InvalidGrantPayload extends RuntimeException {
        private final UUID grantId;

        private InvalidGrantPayload(UUID grantId) {
            this.grantId = grantId;
        }

        private UUID grantId() {
            return grantId;
        }
    }
}
