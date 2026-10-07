package com.ieltspath.library.api.dto.response;

import com.ieltspath.library.application.result.DeckItemResult;
import com.ieltspath.library.domain.vo.FlashcardSourceType;
import com.ieltspath.library.domain.vo.LibraryStatus;

import java.time.Instant;
import java.util.UUID;

public record DeckItemResponse(UUID deckId, UUID flashcardId, Integer sortOrder, Instant addedAt, String front,
                               String back, FlashcardSourceType sourceType, LibraryStatus status) {
    public static DeckItemResponse from(DeckItemResult result) {
        return new DeckItemResponse(result.deckId(), result.flashcardId(), result.sortOrder(), result.addedAt(),
                result.front(), result.back(), result.sourceType(), result.status());
    }
}
