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
@Table(name = "game_events")
public class GameEventJpaEntity {
    @Id
    private UUID id;
    @Column(name = "match_id", nullable = false)
    private UUID matchId;
    @Column(name = "match_player_id")
    private UUID matchPlayerId;
    @Column(name = "sequence_no", nullable = false)
    private long sequenceNo;
    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> payload;
    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected GameEventJpaEntity() {
    }

    public GameEventJpaEntity(UUID id, UUID matchId, UUID matchPlayerId, long sequenceNo, String eventType, Map<String, Object> payload, Instant at) {
        this.id = id;
        this.matchId = matchId;
        this.matchPlayerId = matchPlayerId;
        this.sequenceNo = sequenceNo;
        this.eventType = eventType;
        this.payload = payload;
        this.occurredAt = at;
        this.createdAt = at;
    }
}
