package com.group01.learningsupport.api.dto.response;

import com.group01.learningsupport.application.result.DeckItemResult;
import com.group01.learningsupport.domain.vo.FlashcardSourceType;
import com.group01.learningsupport.domain.vo.LibraryStatus;

import java.time.Instant;
import java.util.UUID;

public record DeckItemResponse(UUID deckId, UUID flashcardId, Integer sortOrder, Instant addedAt, String front,
                               String back, FlashcardSourceType sourceType, LibraryStatus status) {
    public static DeckItemResponse from(DeckItemResult result) {
        return new DeckItemResponse(result.deckId(), result.flashcardId(), result.sortOrder(), result.addedAt(),
                result.front(), result.back(), result.sourceType(), result.status());
    }
}
