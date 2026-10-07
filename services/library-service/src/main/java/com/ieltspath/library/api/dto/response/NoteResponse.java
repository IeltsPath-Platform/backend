package com.ieltspath.library.api.dto.response;

import com.ieltspath.library.application.result.NoteResult;
import com.ieltspath.library.domain.vo.LibraryStatus;
import com.ieltspath.library.domain.vo.NoteSourceType;

import java.time.Instant;
import java.util.UUID;

public record NoteResponse(UUID id, UUID userId, String title, String body, LibraryStatus status,
                           NoteSourceType sourceType, UUID sourceReferenceId,
                           Instant createdAt, Instant updatedAt) {
    public static NoteResponse from(NoteResult result) {
        return new NoteResponse(result.id(), result.userId(), result.title(), result.body(), result.status(),
                result.sourceType(), result.sourceReferenceId(), result.createdAt(), result.updatedAt());
    }
}
