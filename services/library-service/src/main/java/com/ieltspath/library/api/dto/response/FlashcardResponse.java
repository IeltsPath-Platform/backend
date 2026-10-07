package com.ieltspath.library.api.dto.response;

import com.ieltspath.library.application.result.FlashcardResult;
import com.ieltspath.library.domain.vo.FlashcardSourceType;
import com.ieltspath.library.domain.vo.LibraryStatus;

import java.time.Instant;
import java.util.UUID;

public record FlashcardResponse(UUID id, UUID userId, FlashcardSourceType sourceType, UUID vocabularySenseId,
                                UUID sourceReferenceId, String highlightedText, String front, String back,
                                LibraryStatus status, Instant createdAt, Instant updatedAt) {
    public static FlashcardResponse from(FlashcardResult result) {
        return new FlashcardResponse(result.id(), result.userId(), result.sourceType(), result.vocabularySenseId(),
                result.sourceReferenceId(), result.highlightedText(), result.front(), result.back(), result.status(),
                result.createdAt(), result.updatedAt());
    }
}
