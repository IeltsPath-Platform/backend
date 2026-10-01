package com.group01.library.domain.aggregate;

import com.group01.library.domain.vo.FlashcardSourceType;
import com.group01.library.domain.vo.LibraryStatus;

import java.time.Instant;
import java.util.UUID;

public record DeckItem(
        UUID deckId,
        UUID flashcardId,
        Integer sortOrder,
        Instant addedAt,
        String front,
        String back,
        FlashcardSourceType sourceType,
        LibraryStatus status
) {
}
