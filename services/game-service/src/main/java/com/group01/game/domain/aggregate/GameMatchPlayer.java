package com.group01.game.domain.aggregate;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

public record GameMatchPlayer(UUID id, UUID matchId, UUID roomMemberId, UUID userId,
                              int score, Integer rank, Status status, Instant joinedAt, Instant finishedAt) {
    public enum Status { ACTIVE, FINISHED, DISCONNECTED, FORFEITED }
    public GameMatchPlayer withProgress(int newScore, boolean finished, Instant at) {
        return new GameMatchPlayer(id, matchId, roomMemberId, userId, newScore, rank,
                finished ? Status.FINISHED : status, joinedAt, finished ? at : finishedAt);
    }
    public GameMatchPlayer withRank(int newRank) { return new GameMatchPlayer(id, matchId, roomMemberId, userId, score, newRank, status, joinedAt, finishedAt); }
    public long durationMilliseconds() { return finishedAt == null ? Long.MAX_VALUE : Duration.between(joinedAt, finishedAt).toMillis(); }
}
