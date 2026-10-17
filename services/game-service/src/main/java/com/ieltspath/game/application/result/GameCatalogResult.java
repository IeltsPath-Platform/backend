package com.ieltspath.game.application.result;

import java.util.List;

public record GameCatalogResult(List<Item> games) {
    public GameCatalogResult {
        games = List.copyOf(games);
    }

    public record Item(String gameType, String name, String description, String status) {}
}
