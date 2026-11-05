package com.ieltspath.game.api.controller;

import com.ieltspath.game.application.port.WordlyWordProvider;
import com.ieltspath.game.application.usecase.GetWordFiltersUseCase;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WordFiltersController {
    private final GetWordFiltersUseCase filters;

    public WordFiltersController(GetWordFiltersUseCase filters) {
        this.filters = filters;
    }

    @GetMapping("/api/games/word-filters")
    public WordlyWordProvider.Filters filters() {
        return filters.execute();
    }
}
