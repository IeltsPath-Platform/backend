package com.group01.learningsupport.application.result;

import com.group01.learningsupport.domain.aggregate.DeckItem;
import com.group01.learningsupport.domain.vo.FlashcardSourceType;
import com.group01.learningsupport.domain.vo.LibraryStatus;

import java.time.Instant;
import java.util.UUID;

public record DeckItemResult(UUID deckId, UUID flashcardId, Integer sortOrder, Instant addedAt, String front,
                             String back, FlashcardSourceType sourceType, LibraryStatus status) {
    public static DeckItemResult from(DeckItem item) {
        return new DeckItemResult(item.deckId(), item.flashcardId(), item.sortOrder(), item.addedAt(), item.front(),
                item.back(), item.sourceType(), item.status());
    }
}
