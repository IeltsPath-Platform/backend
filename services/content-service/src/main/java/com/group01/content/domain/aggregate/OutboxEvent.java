package com.group01.content.domain.aggregate;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class OutboxEvent {
    private final UUID id;
    private final String aggregateType;
    private final String aggregateId;
    private final String eventType;
    private final String payload;
    private final Instant createdAt;
    private Instant publishedAt;
    private int retryCount;
    private String lastError;

    public OutboxEvent(UUID id, String aggregateType, String aggregateId, String eventType,
                       String payload, Instant createdAt, Instant publishedAt,
                       int retryCount, String lastError) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.aggregateType = Objects.requireNonNull(aggregateType, "aggregateType must not be null");
        this.aggregateId = Objects.requireNonNull(aggregateId, "aggregateId must not be null");
        this.eventType = Objects.requireNonNull(eventType, "eventType must not be null");
        this.payload = Objects.requireNonNull(payload, "payload must not be null");
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.publishedAt = publishedAt;
        this.retryCount = retryCount;
        this.lastError = lastError;
    }

    public static OutboxEvent create(String aggregateType, String aggregateId, String eventType, String payload) {
        return new OutboxEvent(UUID.randomUUID(), aggregateType, aggregateId, eventType, payload,
                Instant.now(), null, 0, null);
    }

    public void markPublished() {
        this.publishedAt = Instant.now();
    }

    public void recordError(String error) {
        this.retryCount++;
        this.lastError = error;
    }

    public UUID getId() { return id; }
    public String getAggregateType() { return aggregateType; }
    public String getAggregateId() { return aggregateId; }
    public String getEventType() { return eventType; }
    public String getPayload() { return payload; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getPublishedAt() { return publishedAt; }
    public int getRetryCount() { return retryCount; }
    public String getLastError() { return lastError; }
}

