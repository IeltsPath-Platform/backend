package com.ieltspath.library.domain.repository;

import com.ieltspath.library.domain.aggregate.Note;
import com.ieltspath.library.domain.vo.LibraryStatus;
import com.ieltspath.library.domain.vo.OwnedPage;
import com.ieltspath.library.domain.vo.NoteSourceType;

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
