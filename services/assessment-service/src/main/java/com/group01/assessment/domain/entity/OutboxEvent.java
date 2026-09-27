package com.group01.assessment.domain.entity;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Integration event recorded in the same transaction as the state change it announces.
 * The id doubles as the published event id so a republish carries the same identity.
 */
public record OutboxEvent(UUID id, String aggregateType, String aggregateId, String eventType, String payload,
                          Instant createdAt, Instant publishedAt, int retryCount, String lastError) {
    public OutboxEvent {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(aggregateType, "aggregateType must not be null");
        Objects.requireNonNull(aggregateId, "aggregateId must not be null");
        Objects.requireNonNull(eventType, "eventType must not be null");
        Objects.requireNonNull(payload, "payload must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
    }

    public static OutboxEvent pending(UUID id, String aggregateType, String aggregateId, String eventType,
                                      String payload, Instant createdAt) {
        return new OutboxEvent(id, aggregateType, aggregateId, eventType, payload, createdAt, null, 0, null);
    }
}
