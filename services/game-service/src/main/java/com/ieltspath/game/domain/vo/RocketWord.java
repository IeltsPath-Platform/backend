package com.ieltspath.game.domain.vo;

import java.util.Locale;
import java.util.UUID;

/** A vocabulary entry as Rocket uses it: the English word is an option, its meaning is the question. */
public record RocketWord(UUID sourceWordId, String word, String meaning) {
    public RocketWord {
        if (sourceWordId == null || word == null || word.isBlank() || meaning == null || meaning.isBlank())
            throw new IllegalArgumentException("A Rocket word needs a source id, a word and a meaning");
    }

    String wordKey() { return word.trim().toLowerCase(Locale.ROOT); }

    String meaningKey() { return meaning.trim().toLowerCase(Locale.ROOT); }
}
