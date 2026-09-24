package com.group01.learningsupport.application.result;

import com.group01.learningsupport.domain.aggregate.Note;
import com.group01.learningsupport.domain.vo.LibraryStatus;

import java.time.Instant;
import java.util.UUID;

public record NoteResult(UUID id, UUID userId, String title, String body, LibraryStatus status,
                         Instant createdAt, Instant updatedAt) {
    public static NoteResult from(Note note) {
        return new NoteResult(note.getId(), note.getUserId(), note.getTitle(), note.getBody(), note.getStatus(),
                note.getCreatedAt(), note.getUpdatedAt());
    }
}
