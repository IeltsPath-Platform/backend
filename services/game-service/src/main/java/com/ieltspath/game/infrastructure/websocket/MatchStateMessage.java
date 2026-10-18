package com.ieltspath.game.infrastructure.websocket;

import com.ieltspath.game.application.result.GameMatchResult;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record MatchStateMessage(String type, Match match) {
    public static MatchStateMessage of(String type, GameMatchResult result) {
        List<Player> players = result.players().stream()
                .map(player -> new Player(player.userId(), player.score(), player.rank(), player.status())).toList();
        return new MatchStateMessage(type, new Match(result.matchId(), result.roomId(), result.sessionId(),
                result.status(), result.startedAt(), players,result.playerMode(),result.settings(),result.ownPlayer(),result.ranking()));
    }

    public record Match(UUID matchId, UUID roomId, UUID sessionId, String status, Instant startedAt,
                        List<Player> players,String playerMode,java.util.Map<String,Object> settings,
                        com.ieltspath.game.application.result.WordlyRankingResult.Player ownPlayer,
                        com.ieltspath.game.application.result.WordlyRankingResult ranking) {
    }

    public record Player(UUID userId, long score, Integer rank, String status) {
    }
}
