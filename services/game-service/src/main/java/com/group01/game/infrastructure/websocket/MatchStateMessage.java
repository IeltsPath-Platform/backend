package com.group01.game.infrastructure.websocket;

import com.group01.game.application.result.GameMatchResult;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record MatchStateMessage(String type, Match match) {
    public static MatchStateMessage of(String type, GameMatchResult result) {
        List<Player> players = result.players().stream()
                .map(player -> new Player(player.userId(), player.score(), player.rank(), player.status())).toList();
        return new MatchStateMessage(type, new Match(result.matchId(), result.roomId(), result.sessionId(),
                result.status(), result.startedAt(), players));
    }

    public record Match(UUID matchId, UUID roomId, UUID sessionId, String status, Instant startedAt,
                        List<Player> players) {
    }

    public record Player(UUID userId, int score, Integer rank, String status) {
    }
}
