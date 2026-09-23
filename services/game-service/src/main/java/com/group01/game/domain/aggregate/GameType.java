package com.group01.game.domain.aggregate;

import java.util.Set;

public enum GameType {
    WORD_MEANING_MATCH("VOCABULARY"),
    SPELLING("VOCABULARY"),
    SENTENCE_COMPLETION("GRAMMAR"),
    ERROR_CORRECTION("GRAMMAR"),
    WORD_ORDER("GRAMMAR");

    private final Set<String> supportedDomains;

    GameType(String domain) { this.supportedDomains = Set.of(domain); }

    public static GameType require(String value, String learningDomain) {
        GameType type;
        try {
            type = GameType.valueOf(value);
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("Unsupported gameType");
        }
        if (!type.supportedDomains.contains(learningDomain)) {
            throw new IllegalArgumentException("gameType is unsupported for learningDomain");
        }
        return type;
    }
}
