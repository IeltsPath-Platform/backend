package com.group01.game.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity @Table(name = "game_matches")
public class GameMatchJpaEntity {
    @Id private UUID id;
    @Column(name = "room_id") private UUID roomId;
    @Column(name = "game_type", nullable = false, length = 50) private String gameType;
    @Column(name = "learning_domain", nullable = false, length = 30) private String learningDomain;
    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "config_snapshot", nullable = false, columnDefinition = "jsonb") private Map<String, Object> configSnapshot;
    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "content_snapshot", nullable = false, columnDefinition = "jsonb") private Map<String, Object> contentSnapshot;
    @Column(nullable = false, length = 30) private String status;
    @Column(name = "started_at") private Instant startedAt;
    @Column(name = "ended_at") private Instant endedAt;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    protected GameMatchJpaEntity() {}
    public GameMatchJpaEntity(UUID id, UUID roomId, String gameType, String learningDomain, Map<String, Object> configSnapshot, Map<String, Object> contentSnapshot, Instant now) {
        this.id=id; this.roomId=roomId; this.gameType=gameType; this.learningDomain=learningDomain; this.configSnapshot=configSnapshot; this.contentSnapshot=contentSnapshot; this.status="IN_PROGRESS"; this.startedAt=now; this.createdAt=now;
    }
    public UUID getId() { return id; }
    public UUID getRoomId() { return roomId; }
    public String getStatus() { return status; }
    public String getGameType() { return gameType; }
    public String getLearningDomain() { return learningDomain; }
    public Map<String,Object> getConfigSnapshot() { return configSnapshot; }
    public Map<String,Object> getContentSnapshot() { return contentSnapshot; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getEndedAt() { return endedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public void complete(Instant at) { this.status="COMPLETED"; this.endedAt=at; }
}
