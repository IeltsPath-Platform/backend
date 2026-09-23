package com.group01.game.application.result;

import com.group01.game.domain.aggregate.GameMatchPlayer;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record GameMatchResult(UUID matchId, UUID roomId, UUID sessionId, String status, Instant startedAt,
                              List<PlayerResult> players) {
    public GameMatchResult { players=List.copyOf(players); }
    public record PlayerResult(UUID userId, int score, Integer rank, String status) {}
    public static List<PlayerResult> players(List<GameMatchPlayer> players) {
        return players.stream().map(p->new PlayerResult(p.userId(),p.score(),p.rank(),p.status().name())).toList();
    }
}
