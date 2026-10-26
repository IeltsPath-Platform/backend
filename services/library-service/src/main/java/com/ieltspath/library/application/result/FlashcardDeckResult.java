package com.ieltspath.library.application.result;

import com.ieltspath.library.domain.aggregate.FlashcardDeck;
import com.ieltspath.library.domain.vo.LibraryStatus;

import java.time.Instant;
import java.util.UUID;

public record FlashcardDeckResult(
        UUID id,
        UUID userId,
        String name,
        String description,
        LibraryStatus status,
        Instant createdAt,
        Instant updatedAt,
        String coverImageKey,
        String coverImageUrl,
        long cardCount,
        long dueCount
) {
    public static FlashcardDeckResult from(FlashcardDeck deck, String coverImageUrl, long cardCount, long dueCount) {
        return new FlashcardDeckResult(
                deck.getId(),
                deck.getUserId(),
                deck.getName(),
                deck.getDescription(),
                deck.getStatus(),
                deck.getCreatedAt(),
                deck.getUpdatedAt(),
                deck.getCoverImageKey(),
                coverImageUrl,
                cardCount,
                dueCount
        );
    }

    public static FlashcardDeckResult from(FlashcardDeck deck) {
        return from(deck, null, 0, 0);
    }

    public FlashcardDeckResult(
            UUID id,
            UUID userId,
            String name,
            String description,
            LibraryStatus status,
            Instant createdAt,
            Instant updatedAt
    ) {
        this(id, userId, name, description, status, createdAt, updatedAt, null, null, 0, 0);
    }
}
