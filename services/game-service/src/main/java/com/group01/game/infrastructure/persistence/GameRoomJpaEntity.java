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

@Entity
@Table(name = "game_rooms")
public class GameRoomJpaEntity {
    @Id private UUID id;
    @Column(name = "room_code", nullable = false, unique = true, length = 20) private String roomCode;
    @Column(name = "host_user_id", nullable = false) private UUID hostUserId;
    @Column(name = "game_type", nullable = false, length = 50) private String gameType;
    @Column(name = "learning_domain", nullable = false, length = 30) private String learningDomain;
    @Column(nullable = false, length = 30) private String mode;
    @Column(name = "max_players", nullable = false) private int maxPlayers;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "config_snapshot", nullable = false, columnDefinition = "jsonb") private Map<String, Object> configSnapshot;
    @Column(nullable = false, length = 30) private String status;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @Column(name = "expires_at") private Instant expiresAt;

    protected GameRoomJpaEntity() {}
    public GameRoomJpaEntity(UUID id, String roomCode, UUID hostUserId, String gameType, String learningDomain,
                             String mode, int maxPlayers, Map<String, Object> configSnapshot, Instant now, Instant expiresAt) {
        this.id = id; this.roomCode = roomCode; this.hostUserId = hostUserId; this.gameType = gameType;
        this.learningDomain = learningDomain; this.mode = mode; this.maxPlayers = maxPlayers;
        this.configSnapshot = configSnapshot; this.status = "WAITING"; this.createdAt = now;
        this.updatedAt = now; this.expiresAt = expiresAt;
    }
    public UUID getId() { return id; }
    public String getRoomCode() { return roomCode; }
    public UUID getHostUserId() { return hostUserId; }
    public String getGameType() { return gameType; }
    public String getLearningDomain() { return learningDomain; }
    public String getMode() { return mode; }
    public int getMaxPlayers() { return maxPlayers; }
    public Map<String, Object> getConfigSnapshot() { return configSnapshot; }
    public String getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setStatus(String status, Instant updatedAt) { this.status = status; this.updatedAt = updatedAt; }
}
