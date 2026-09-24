package com.group01.game.api.dto.response;

import com.group01.game.application.result.GameHistoryResult;

import java.util.List;

public record GameHistoryResponse(List<GameSessionResponse> content, int page, int size,
                                  long totalElements, long totalPages) {
    public GameHistoryResponse {
        content = List.copyOf(content);
    }

    public static GameHistoryResponse from(GameHistoryResult result) {
        return new GameHistoryResponse(result.content().stream().map(GameSessionResponse::from).toList(),
                result.page(), result.size(), result.totalElements(), result.totalPages());
    }
}
