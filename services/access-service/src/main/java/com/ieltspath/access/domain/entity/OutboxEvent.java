package com.ieltspath.access.domain.entity;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public class OutboxEvent {

    /** Event types the relay publishes; any other type is kept as a record only and never leaves the service. */
    public static final Set<String> RELAYED_EVENT_TYPES = Set.of("SubscriptionGranted");

    private final UUID id;
    private final String aggregateType;
    private final UUID aggregateId;
    private final String eventType;
    private final String payload;
    private String status;
    private int retryCount;
    private final Instant createdAt;
    private Instant publishedAt;
    private final String correlationId;
    private final String traceContext;

    public OutboxEvent(UUID id, String aggregateType, UUID aggregateId, String eventType, String payload, String status, int retryCount, Instant createdAt, Instant publishedAt) {
        this(id, aggregateType, aggregateId, eventType, payload, status, retryCount, createdAt, publishedAt, null);
    }

    public OutboxEvent(UUID id, String aggregateType, UUID aggregateId, String eventType, String payload, String status,
                       int retryCount, Instant createdAt, Instant publishedAt, String correlationId) {
        this(id, aggregateType, aggregateId, eventType, payload, status, retryCount, createdAt, publishedAt,
                correlationId, null);
    }

    public OutboxEvent(UUID id, String aggregateType, UUID aggregateId, String eventType, String payload, String status,
                       int retryCount, Instant createdAt, Instant publishedAt, String correlationId, String traceContext) {
        this.id = id;
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.payload = payload;
        this.status = status;
        this.retryCount = retryCount;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.publishedAt = publishedAt;
        this.correlationId = correlationId;
        this.traceContext = traceContext;
    }

    public static OutboxEvent create(String aggregateType, UUID aggregateId, String eventType, String payload) {
        return create(UUID.randomUUID(), aggregateType, aggregateId, eventType, payload);
    }

    /**
     * The id is supplied by the caller because the payload carries it as the event id. A type outside
     * {@link #RELAYED_EVENT_TYPES} is stored as NOT_RELAYED, so the relay never picks it up.
     */
    public static OutboxEvent create(UUID id, String aggregateType, UUID aggregateId, String eventType, String payload) {
        String status = RELAYED_EVENT_TYPES.contains(eventType) ? "PENDING" : "NOT_RELAYED";
        return new OutboxEvent(id, aggregateType, aggregateId, eventType, payload, status, 0, Instant.now(), null);
    }

    public UUID getId() {
        return id;
    }

    public String getAggregateType() {
        return aggregateType;
    }

    public UUID getAggregateId() {
        return aggregateId;
    }

    public String getEventType() {
        return eventType;
    }

    public String getPayload() {
        return payload;
    }

    public String getStatus() {
        return status;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public String getCorrelationId() { return correlationId; }
    public String getTraceContext() { return traceContext; }

    public void markPublished() {
        this.status = "PUBLISHED";
        this.publishedAt = Instant.now();
    }

    public void markFailed() {
        this.status = "FAILED";
        this.retryCount++;
    }
}
