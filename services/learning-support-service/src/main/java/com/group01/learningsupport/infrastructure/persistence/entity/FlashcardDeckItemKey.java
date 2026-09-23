package com.group01.learningsupport.infrastructure.persistence.entity;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class FlashcardDeckItemKey implements Serializable {
    private UUID deckId;
    private UUID flashcardId;

    public FlashcardDeckItemKey() {
    }

    public FlashcardDeckItemKey(UUID deckId, UUID flashcardId) {
        this.deckId = deckId;
        this.flashcardId = flashcardId;
    }

    public UUID getDeckId() {
        return deckId;
    }

    public UUID getFlashcardId() {
        return flashcardId;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof FlashcardDeckItemKey key)) {
            return false;
        }
        return Objects.equals(deckId, key.deckId) && Objects.equals(flashcardId, key.flashcardId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(deckId, flashcardId);
    }
}
