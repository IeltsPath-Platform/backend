package com.ieltspath.access.infrastructure.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.access.application.result.PointCreditResult;
import com.ieltspath.access.application.usecase.CreditPointsUseCase;
import com.ieltspath.access.domain.exception.DuplicateIdempotencyException;
import com.ieltspath.access.domain.vo.LearningRewardReason;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

@Component
public class LearningPointsGrantListener {
    private final ObjectMapper objectMapper;
    private final CreditPointsUseCase creditPoints;
    private final RabbitTemplate rabbitTemplate;

    public LearningPointsGrantListener(ObjectMapper objectMapper,
                                       CreditPointsUseCase creditPoints,
                                       RabbitTemplate rabbitTemplate) {
        this.objectMapper = objectMapper;
        this.creditPoints = creditPoints;
        this.rabbitTemplate = rabbitTemplate;
    }

    @RabbitListener(queues = LearningPointsMessagingConfig.GRANT_QUEUE)
    public void onPointsGrantRequested(Message message) {
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

            String reasonValue = text(root, "reason");
            if (!LearningRewardReason.STREAK_MILESTONE.name().equals(reasonValue)) {
                throw new InvalidGrantPayload(grantId);
            }
            String referenceId = text(root, "referenceId");
            if (referenceId.isBlank() || referenceId.length() > 100) throw new InvalidGrantPayload(grantId);

            return new GrantRequest(grantId, userId, amount, LearningRewardReason.STREAK_MILESTONE, referenceId);
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
        rabbitTemplate.convertAndSend(LearningPointsMessagingConfig.EXCHANGE,
                LearningPointsMessagingConfig.SETTLED_ROUTING_KEY, settled);
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
