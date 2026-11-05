package com.ieltspath.game.domain.exception;

/** The room's level and topic leave too few words for its settings. */
public class NotEnoughWordsException extends SoloGameplayException {
    private final int available;

    public NotEnoughWordsException(int available) {
        super(409, "NOT_ENOUGH_WORDS", "Not enough words for this level and topic");
        this.available = available;
    }

    public int available() { return available; }
}
