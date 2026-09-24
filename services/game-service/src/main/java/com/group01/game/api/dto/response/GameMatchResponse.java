package com.group01.game.api.dto.response;

import com.group01.game.application.result.GameMatchResult;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record GameMatchResponse(UUID matchId, UUID roomId, UUID sessionId, String status, Instant startedAt,
                                List<GameMatchPlayerResponse> players) {
    public GameMatchResponse {
        players = List.copyOf(players);
    }

    public static GameMatchResponse from(GameMatchResult result) {
        return new GameMatchResponse(result.matchId(), result.roomId(), result.sessionId(), result.status(),
                result.startedAt(), result.players().stream().map(GameMatchPlayerResponse::from).toList());
    }
}
