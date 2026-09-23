package com.group01.game.domain.aggregate;

import com.group01.game.domain.exception.InvalidGameSessionStateException;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public class GameSession {
    private final UUID id;
    private final UUID userId;
    private final UUID topicId;
    private final UUID matchPlayerId;
    private final String gameType;
    private final String learningDomain;
    private final String mode;
    private final Instant startedAt;
    private final int itemCount;
    private final Map<String, Object> sourceSnapshot;
    private final String verificationStatus;
    private int answeredCount;
    private int score;
    private GameSessionStatus status;
    private Instant endedAt;

    public GameSession(UUID id, UUID userId, UUID topicId, UUID matchPlayerId, String gameType, String learningDomain,
                       String mode, Instant startedAt, Map<String, Object> sourceSnapshot, String verificationStatus) {
        int itemCount = ((java.util.List<?>) sourceSnapshot.getOrDefault("items", java.util.List.of())).size();
        if (itemCount < 1) throw new IllegalArgumentException("A game session must contain at least one item");
        this.id = id;
        this.userId = userId;
        this.topicId = topicId;
        this.matchPlayerId = matchPlayerId;
        this.gameType = gameType;
        this.learningDomain = learningDomain;
        this.mode = mode;
        this.startedAt = startedAt;
        this.itemCount = itemCount;
        this.sourceSnapshot = Map.copyOf(sourceSnapshot);
        this.verificationStatus = verificationStatus;
        this.status = GameSessionStatus.IN_PROGRESS;
    }

    public static GameSession reconstitute(UUID id, UUID userId, UUID topicId, UUID matchPlayerId,
                                           String gameType, String learningDomain, String mode, Instant startedAt,
                                           Instant endedAt, int score, int answeredCount,
                                           Map<String, Object> sourceSnapshot, String verificationStatus,
                                           GameSessionStatus status) {
        GameSession session = new GameSession(id, userId, topicId, matchPlayerId, gameType, learningDomain, mode,
                startedAt, sourceSnapshot, verificationStatus);
        if (answeredCount < 0 || answeredCount > session.itemCount || score < 0 || score > answeredCount) {
            throw new IllegalArgumentException("Persisted game session counters are invalid");
        }
        session.endedAt = endedAt;
        session.score = score;
        session.answeredCount = answeredCount;
        session.status = status;
        return session;
    }

    public void recordAnswer(boolean correct, Instant answeredAt) {
        if (status != GameSessionStatus.IN_PROGRESS) throw new InvalidGameSessionStateException("Session is not in progress");
        if (answeredCount >= itemCount) throw new InvalidGameSessionStateException("All session items have already been answered");
        answeredCount++;
        if (correct) score++;
        if (answeredCount == itemCount) complete(answeredAt);
    }

    public void complete(Instant at) {
        if (status != GameSessionStatus.IN_PROGRESS) throw new InvalidGameSessionStateException("Session is not in progress");
        status = GameSessionStatus.COMPLETED;
        endedAt = at;
    }

    public void abandon(Instant at) {
        if (status != GameSessionStatus.IN_PROGRESS) throw new InvalidGameSessionStateException("Session is not in progress");
        status = GameSessionStatus.ABANDONED;
        endedAt = at;
    }

    public UUID id() { return id; }
    public UUID userId() { return userId; }
    public UUID topicId() { return topicId; }
    public UUID matchPlayerId() { return matchPlayerId; }
    public String gameType() { return gameType; }
    public String learningDomain() { return learningDomain; }
    public String mode() { return mode; }
    public Instant startedAt() { return startedAt; }
    public Instant endedAt() { return endedAt; }
    public int itemCount() { return itemCount; }
    public Map<String, Object> sourceSnapshot() { return sourceSnapshot; }
    public String verificationStatus() { return verificationStatus; }
    public int answeredCount() { return answeredCount; }
    public int score() { return score; }
    public GameSessionStatus status() { return status; }
}
