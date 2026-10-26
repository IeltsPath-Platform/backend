package com.ieltspath.library.api.dto.response;

import com.ieltspath.library.application.result.FlashcardDeckResult;
import com.ieltspath.library.domain.vo.LibraryStatus;

import java.time.Instant;
import java.util.UUID;

public record FlashcardDeckResponse(
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
    public static FlashcardDeckResponse from(FlashcardDeckResult result) {
        return new FlashcardDeckResponse(
                result.id(),
                result.userId(),
                result.name(),
                result.description(),
                result.status(),
                result.createdAt(),
                result.updatedAt(),
                result.coverImageKey(),
                result.coverImageUrl(),
                result.cardCount(),
                result.dueCount()
        );
    }
}
