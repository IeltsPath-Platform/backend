package com.group01.game.api.dto.response;

import com.group01.game.application.result.GameMatchResult;

import java.util.UUID;

public record GameMatchPlayerResponse(UUID userId, int score, Integer rank, String status) {
    public static GameMatchPlayerResponse from(GameMatchResult.PlayerResult result) {
        return new GameMatchPlayerResponse(result.userId(), result.score(), result.rank(), result.status());
    }
}
