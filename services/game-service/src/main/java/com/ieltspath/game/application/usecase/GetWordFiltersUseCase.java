package com.ieltspath.game.application.usecase;

import com.ieltspath.game.application.port.WordlyWordProvider;
import org.springframework.stereotype.Service;

/** The topics a room can choose and how many words each level and topic holds, from Library. */
@Service
public class GetWordFiltersUseCase {
    private final WordlyWordProvider words;

    public GetWordFiltersUseCase(WordlyWordProvider words) {
        this.words = words;
    }

    public WordlyWordProvider.Filters execute() {
        return words.filters();
    }
}
