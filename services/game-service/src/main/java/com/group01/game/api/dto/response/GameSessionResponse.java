package com.group01.game.api.dto.response;

import com.group01.game.application.result.GameSessionResult;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record GameSessionResponse(UUID id, String gameType, String learningDomain, String mode, UUID topicId,
                                  String status, int score, int itemCount, int answeredCount,
                                  Instant startedAt, Instant endedAt, List<GameItemResponse> items) {
    public GameSessionResponse {
        items = List.copyOf(items);
    }

    public static GameSessionResponse from(GameSessionResult result) {
        return new GameSessionResponse(result.id(), result.gameType(), result.learningDomain(), result.mode(),
                result.topicId(), result.status(), result.score(), result.itemCount(), result.answeredCount(),
                result.startedAt(), result.endedAt(), result.items().stream().map(GameItemResponse::from).toList());
    }
}
