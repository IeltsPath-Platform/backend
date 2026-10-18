package com.ieltspath.game.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.hibernate.annotations.SQLRestriction;
import com.ieltspath.game.domain.vo.WordlyRoomSettings;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "game_rooms")
@SQLRestriction("is_delete = false")
public class GameRoomJpaEntity {
    @Id
    private UUID id;
    @Column(name = "room_code", nullable = false, unique = true, length = 20)
    private String roomCode;
    @Column(name = "host_user_id", nullable = false)
    private UUID hostUserId;
    @Column(name = "game_type", nullable = false, length = 50)
    private String gameType;
    @Column(name = "learning_domain", nullable = false, length = 30)
    private String learningDomain;
    @Column(nullable = false, length = 30)
    private String mode;
    @Column(name = "max_players", nullable = false)
    private int maxPlayers;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "config_snapshot", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> configSnapshot;
    @Column(nullable = false, length = 30)
    private String status;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    @Column(name = "expires_at")
    private Instant expiresAt;
    @Column(name = "player_mode", nullable = false)
    private String playerMode = "MULTIPLAYER";
    @Column(name = "initial_lives", nullable = false)
    private short initialLives = 4;
    @Column(name = "word_time_limit_seconds")
    private Short wordTimeLimitSeconds = 180;
    @Column(name = "play_mode", nullable = false)
    private String playMode = "FINITE";
    @Column(name = "word_limit")
    private Integer wordLimit = 10;
    @Column(name = "is_delete", nullable = false)
    private boolean isDelete;
    @Column(name = "deleted_at")
    private Instant deletedAt;

    public WordlyRoomSettings wordlySettings() {
        return "WORDLY".equals(gameType) ? new WordlyRoomSettings(
                WordlyRoomSettings.PlayerMode.valueOf(playerMode), initialLives,
                WordlyRoomSettings.PlayMode.valueOf(playMode), wordLimit, wordTimeLimitSeconds != null) : null;
    }

    public void setWordlySettings(WordlyRoomSettings settings) {
        if (settings == null) return;
        playerMode = settings.playerMode().name();
        initialLives = (short) settings.initialLives();
        wordTimeLimitSeconds = settings.timeEnabled() ? (short) 180 : null;
        playMode = settings.playMode().name();
        wordLimit = settings.wordLimit();
    }

    protected GameRoomJpaEntity() {
    }

    public GameRoomJpaEntity(UUID id, String roomCode, UUID hostUserId, String gameType, String learningDomain,
                             String mode, int maxPlayers, Map<String, Object> configSnapshot, Instant now, Instant expiresAt) {
        this.id = id;
        this.roomCode = roomCode;
        this.hostUserId = hostUserId;
        this.gameType = gameType;
        this.learningDomain = learningDomain;
        this.mode = mode;
        this.maxPlayers = maxPlayers;
        this.configSnapshot = configSnapshot;
        this.status = "WAITING";
        this.createdAt = now;
        this.updatedAt = now;
        this.expiresAt = expiresAt;
    }

    public UUID getId() {
        return id;
    }

    public String getRoomCode() {
        return roomCode;
    }

    public UUID getHostUserId() {
        return hostUserId;
    }

    public String getGameType() {
        return gameType;
    }

    public String getLearningDomain() {
        return learningDomain;
    }

    public String getMode() {
        return mode;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public Map<String, Object> getConfigSnapshot() {
        return configSnapshot;
    }

    public String getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setStatus(String status, Instant updatedAt) {
        this.status = status;
        this.updatedAt = updatedAt;
        if ("CLOSED".equals(status)) { this.isDelete = true; this.deletedAt = updatedAt; }
    }
}
