package com.group01.learningsupport.api.dto.response;

import com.group01.learningsupport.application.result.NoteResult;
import com.group01.learningsupport.domain.vo.LibraryStatus;

import java.time.Instant;
import java.util.UUID;

public record NoteResponse(UUID id, UUID userId, String title, String body, LibraryStatus status,
                           Instant createdAt, Instant updatedAt) {
    public static NoteResponse from(NoteResult result) {
        return new NoteResponse(result.id(), result.userId(), result.title(), result.body(), result.status(),
                result.createdAt(), result.updatedAt());
    }
}
