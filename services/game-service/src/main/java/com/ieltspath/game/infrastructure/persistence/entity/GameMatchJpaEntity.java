package com.ieltspath.game.infrastructure.persistence.entity;

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
@Table(name = "game_matches")
public class GameMatchJpaEntity {
    @Id
    private UUID id;
    @Column(name = "room_id")
    private UUID roomId;
    @Column(name = "game_type", nullable = false, length = 50)
    private String gameType;
    @Column(name = "learning_domain", nullable = false, length = 30)
    private String learningDomain;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "config_snapshot", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> configSnapshot;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "content_snapshot", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> contentSnapshot;
    @Column(nullable = false, length = 30)
    private String status;
    @Column(name = "started_at")
    private Instant startedAt;
    @Column(name = "ended_at")
    private Instant endedAt;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "player_mode", nullable = false) private String playerMode = "MULTIPLAYER";
    @Column(name = "play_mode", nullable = false) private String playMode = "FINITE";
    @Column(name = "initial_lives", nullable = false) private short initialLives = 4;
    @Column(name = "word_time_limit_seconds") private Short wordTimeLimitSeconds = 180;
    @Column(name = "word_limit") private Integer wordLimit = 10;
    @Column(name = "question_count") private Short questionCount;
    @Column(name = "seconds_per_question") private Short secondsPerQuestion;

    protected GameMatchJpaEntity() {
    }

    public GameMatchJpaEntity(UUID id, UUID roomId, String gameType, String learningDomain, Map<String, Object> configSnapshot, Map<String, Object> contentSnapshot, Instant now) {
        this.id = id;
        this.roomId = roomId;
        this.gameType = gameType;
        this.learningDomain = learningDomain;
        this.configSnapshot = configSnapshot;
        if ("WORDLY".equals(gameType)) {
            wordTimeLimitSeconds = Boolean.FALSE.equals(configSnapshot.get("timeEnabled")) ? null : (short) 180;
            playerMode = (String) configSnapshot.get("playerMode");
            playMode = (String) configSnapshot.get("playMode");
            initialLives = ((Number) configSnapshot.get("initialLives")).shortValue();
            wordLimit = (Integer) configSnapshot.get("wordLimit");
        }
        if ("ROCKET".equals(gameType)) {
            playerMode = (String) configSnapshot.get("playerMode");
            initialLives = ((Number) configSnapshot.get("initialLives")).shortValue();
            questionCount = ((Number) configSnapshot.get("questionCount")).shortValue();
            secondsPerQuestion = ((Number) configSnapshot.get("secondsPerQuestion")).shortValue();
        }
        this.contentSnapshot = contentSnapshot;
        this.status = "IN_PROGRESS";
        this.startedAt = now;
        this.createdAt = now;
    }

    public UUID getId() {
        return id;
    }

    public UUID getRoomId() {
        return roomId;
    }

    public String getStatus() {
        return status;
    }

    public String getGameType() {
        return gameType;
    }

    public String getLearningDomain() {
        return learningDomain;
    }

    public Map<String, Object> getConfigSnapshot() {
        return configSnapshot;
    }

    public Map<String, Object> getContentSnapshot() {
        return contentSnapshot;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getEndedAt() {
        return endedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void complete(Instant at) {
        this.status = "COMPLETED";
        this.endedAt = at;
    }
}
