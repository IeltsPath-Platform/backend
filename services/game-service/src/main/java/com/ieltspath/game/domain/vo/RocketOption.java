package com.ieltspath.game.domain.vo;

import java.util.UUID;

/** One planet in a Rocket round. Clients see the option id and word, never which option is correct. */
public record RocketOption(UUID optionId, UUID sourceWordId, String word) {
    public RocketOption {
        if (optionId == null || sourceWordId == null || word == null || word.isBlank())
            throw new IllegalArgumentException("An option needs an id, a source word and its text");
    }
}
