package com.ieltspath.library.domain.vo;

public record DeckCardCounts(
        long cardCount,
        long dueCount
) {
    public static final DeckCardCounts ZERO = new DeckCardCounts(0, 0);
}
