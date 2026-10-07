package com.ieltspath.library.application.result;

import com.ieltspath.library.domain.aggregate.FlashcardDeck;
import com.ieltspath.library.domain.vo.LibraryStatus;

import java.time.Instant;
import java.util.UUID;

public record FlashcardDeckResult(UUID id, UUID userId, String name, String description, LibraryStatus status,
                                  Instant createdAt, Instant updatedAt) {
    public static FlashcardDeckResult from(FlashcardDeck deck) {
        return new FlashcardDeckResult(deck.getId(), deck.getUserId(), deck.getName(), deck.getDescription(),
                deck.getStatus(), deck.getCreatedAt(), deck.getUpdatedAt());
    }
}
