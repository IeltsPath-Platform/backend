package com.ieltspath.game.api.controller;

import com.ieltspath.game.api.dto.response.GameCatalogResponse;
import com.ieltspath.game.application.usecase.GetGameCatalogUseCase;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/games/catalog")
public class GameCatalogController {
    private final GetGameCatalogUseCase getCatalog;

    public GameCatalogController(GetGameCatalogUseCase getCatalog) {
        this.getCatalog = getCatalog;
    }

    @GetMapping
    public GameCatalogResponse get() {
        return GameCatalogResponse.from(getCatalog.execute());
    }
}
