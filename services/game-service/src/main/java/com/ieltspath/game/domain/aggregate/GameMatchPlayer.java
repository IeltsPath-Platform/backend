package com.ieltspath.game.domain.aggregate;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

public record GameMatchPlayer(UUID id, UUID matchId, UUID roomMemberId, UUID userId,
                              long score, Integer rank, Status status, Instant joinedAt, Instant finishedAt, int livesRemaining) {
    public GameMatchPlayer(UUID id, UUID matchId, UUID roomMemberId, UUID userId, long score, Integer rank, Status status, Instant joinedAt, Instant finishedAt) {
        this(id, matchId, roomMemberId, userId, score, rank, status, joinedAt, finishedAt, 4);
    }
    public enum Status { ACTIVE, FINISHED, DISCONNECTED, FORFEITED, ELIMINATED }
    public GameMatchPlayer withProgress(long newScore, boolean finished, Instant at) {
        return new GameMatchPlayer(id, matchId, roomMemberId, userId, newScore, rank,
                finished ? Status.FINISHED : status, joinedAt, finished ? at : finishedAt, livesRemaining);
    }
    public GameMatchPlayer withRank(int newRank) { return new GameMatchPlayer(id, matchId, roomMemberId, userId, score, newRank, status, joinedAt, finishedAt, livesRemaining); }
    public long durationMilliseconds() { return finishedAt == null ? Long.MAX_VALUE : Duration.between(joinedAt, finishedAt).toMillis(); }
}
