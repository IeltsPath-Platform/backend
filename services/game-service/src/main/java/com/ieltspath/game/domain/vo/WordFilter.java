package com.ieltspath.game.domain.vo;

import java.util.Set;
import java.util.UUID;

/** Which words a match may use. A null level or topic means any; the topic is checked by Library when the match starts. */
public record WordFilter(String cefrLevel, UUID topicId) {
    public static final WordFilter ANY = new WordFilter(null, null);
    private static final Set<String> LEVELS = Set.of("A1", "A2", "B1", "B2", "C1", "C2");

    public WordFilter {
        if (cefrLevel != null && !LEVELS.contains(cefrLevel)) throw new IllegalArgumentException("Unknown CEFR level");
    }

    public boolean isAny() { return cefrLevel == null && topicId == null; }
}
