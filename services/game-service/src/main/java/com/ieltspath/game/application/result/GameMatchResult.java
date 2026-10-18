package com.ieltspath.game.application.result;

import com.ieltspath.game.domain.aggregate.GameMatchPlayer;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record GameMatchResult(UUID matchId, UUID roomId, UUID sessionId, String status, Instant startedAt,
                              List<PlayerResult> players, String playerMode, java.util.Map<String,Object> settings,
                              WordlyRankingResult.Player ownPlayer, WordlyRankingResult ranking) {
    public GameMatchResult(UUID matchId, UUID roomId, UUID sessionId, String status, Instant startedAt, List<PlayerResult> players) {
        this(matchId,roomId,sessionId,status,startedAt,players,null,null,null,null);
    }
    public GameMatchResult { players=List.copyOf(players); }
    public record PlayerResult(UUID userId, long score, Integer rank, String status) {}
    public static List<PlayerResult> players(List<GameMatchPlayer> players) {
        return players.stream().map(p->new PlayerResult(p.userId(),p.score(),p.rank(),p.status().name())).toList();
    }
}
