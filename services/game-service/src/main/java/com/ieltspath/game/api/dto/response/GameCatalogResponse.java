package com.ieltspath.game.api.dto.response;

import com.ieltspath.game.application.result.GameCatalogResult;

import java.util.List;

public record GameCatalogResponse(List<ItemResponse> games) {
    public GameCatalogResponse {
        games = List.copyOf(games);
    }

    public static GameCatalogResponse from(GameCatalogResult result) {
        return new GameCatalogResponse(result.games().stream()
                .map(item -> new ItemResponse(item.gameType(), item.name(), item.description(), item.status()))
                .toList());
    }

    public record ItemResponse(String gameType, String name, String description, String status) {}
}
