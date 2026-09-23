package com.group01.learningsupport.domain.repository;

import com.group01.learningsupport.domain.aggregate.Note;
import com.group01.learningsupport.domain.vo.LibraryStatus;
import com.group01.learningsupport.domain.vo.OwnedPage;

import java.util.Optional;
import java.util.UUID;

public interface NoteRepository {
    Note save(Note note);

    Optional<Note> findByIdAndUserId(UUID id, UUID userId);

    OwnedPage<Note> findByUserIdAndStatus(UUID userId, LibraryStatus status, int page, int size);
}
