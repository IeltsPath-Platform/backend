package com.ieltspath.user.domain.aggregate;

import java.time.Instant;
import java.util.UUID;

public record OutboxEvent(UUID id, String aggregateType, UUID aggregateId, String eventType, String payload,
                         String status, int retryCount, Instant createdAt, Instant publishedAt,
                         String correlationId, String traceContext) {
    public static OutboxEvent create(UUID id, UUID aggregateId, String eventType, String payload) {
        return new OutboxEvent(id, "User", aggregateId, eventType, payload, "PENDING", 0, Instant.now(), null,
                null, null);
    }
}
