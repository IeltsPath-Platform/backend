package com.group01.library.domain.repository;

import com.group01.library.domain.aggregate.Note;
import com.group01.library.domain.vo.LibraryStatus;
import com.group01.library.domain.vo.OwnedPage;
import com.group01.library.domain.vo.NoteSourceType;

import java.util.Optional;
import java.util.UUID;

public interface NoteRepository {
    Note save(Note note);

    Optional<Note> findByIdAndUserId(UUID id, UUID userId);

    OwnedPage<Note> findByUserIdAndStatus(UUID userId, LibraryStatus status, int page, int size);

    OwnedPage<Note> findByUserIdAndStatusAndSourceType(
            UUID userId, LibraryStatus status, NoteSourceType sourceType, int page, int size);

    OwnedPage<Note> findByUserIdAndStatusAndSourceTypeAndSourceReferenceId(
            UUID userId, LibraryStatus status, NoteSourceType sourceType,
            UUID sourceReferenceId, int page, int size);
}
