package com.group01.game.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "outbox_events")
public class OutboxEventJpaEntity {
    @Id
    private UUID id;
    @Column(name = "aggregate_type", nullable = false, length = 100)
    private String aggregateType;
    @Column(name = "aggregate_id", nullable = false, length = 255)
    private String aggregateId;
    @Column(name = "event_type", nullable = false, length = 150)
    private String eventType;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> payload;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "published_at")
    private Instant publishedAt;
    @Column(name = "retry_count", nullable = false)
    private int retryCount;
    @Column(name = "last_error")
    private String lastError;

    protected OutboxEventJpaEntity() {
    }

    public OutboxEventJpaEntity(UUID id, String aggregateType, String aggregateId, String eventType,
                                Map<String, Object> payload, Instant createdAt) {
        this.id = id;
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.payload = payload;
        this.createdAt = createdAt;
        this.retryCount = 0;
    }
}
