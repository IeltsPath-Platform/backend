package com.group01.learningsupport.api.dto.response;

import com.group01.learningsupport.application.result.FlashcardDeckResult;
import com.group01.learningsupport.domain.vo.LibraryStatus;

import java.time.Instant;
import java.util.UUID;

public record FlashcardDeckResponse(UUID id, UUID userId, String name, String description, LibraryStatus status,
                                    Instant createdAt, Instant updatedAt) {
    public static FlashcardDeckResponse from(FlashcardDeckResult result) {
        return new FlashcardDeckResponse(result.id(), result.userId(), result.name(), result.description(),
                result.status(), result.createdAt(), result.updatedAt());
    }
}
