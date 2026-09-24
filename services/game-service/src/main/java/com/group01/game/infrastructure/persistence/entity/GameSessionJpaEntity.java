package com.group01.game.infrastructure.persistence.entity;

import com.group01.game.domain.aggregate.GameSessionStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "game_sessions")
public class GameSessionJpaEntity {
    @Id
    private UUID id;
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    @Column(name = "match_player_id", unique = true)
    private UUID matchPlayerId;
    @Column(name = "game_type", nullable = false, length = 50)
    private String gameType;
    @Column(name = "learning_domain", nullable = false, length = 30)
    private String learningDomain;
    @Column(nullable = false, length = 30)
    private String mode;
    @Column(name = "topic_id")
    private UUID topicId;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "source_snapshot", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> sourceSnapshot;
    @Column(name = "started_at", nullable = false)
    private Instant startedAt;
    @Column(name = "ended_at")
    private Instant endedAt;
    private Integer score;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private GameSessionStatus status;
    @Column(name = "verification_status", nullable = false, length = 30)
    private String verificationStatus;

    protected GameSessionJpaEntity() {
    }

    public GameSessionJpaEntity(UUID id, UUID userId, UUID matchPlayerId, String gameType, String learningDomain, String mode,
                                UUID topicId, Map<String, Object> sourceSnapshot, Instant startedAt) {
        this.id = id;
        this.userId = userId;
        this.matchPlayerId = matchPlayerId;
        this.gameType = gameType;
        this.learningDomain = learningDomain;
        this.mode = mode;
        this.topicId = topicId;
        this.sourceSnapshot = sourceSnapshot;
        this.startedAt = startedAt;
        this.status = GameSessionStatus.IN_PROGRESS;
        this.verificationStatus = "PENDING";
        this.score = 0;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
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

    public UUID getTopicId() {
        return topicId;
    }

    public Map<String, Object> getSourceSnapshot() {
        return sourceSnapshot;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getEndedAt() {
        return endedAt;
    }

    public Integer getScore() {
        return score;
    }

    public GameSessionStatus getStatus() {
        return status;
    }

    public String getVerificationStatus() {
        return verificationStatus;
    }

    public void incrementScore(boolean correct) {
        if (correct) score = score + 1;
    }

    public void setProgress(int score, GameSessionStatus status, Instant endedAt) {
        this.score = score;
        this.status = status;
        this.endedAt = endedAt;
    }

    public UUID getMatchPlayerId() {
        return matchPlayerId;
    }

    public void complete(Instant at) {
        status = GameSessionStatus.COMPLETED;
        endedAt = at;
    }

    public void abandon(Instant at) {
        status = GameSessionStatus.ABANDONED;
        endedAt = at;
    }
}
