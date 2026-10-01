package com.group01.library.application.usecase;

import com.group01.library.application.ApplicationSupport;
import com.group01.library.application.result.NoteResult;
import com.group01.library.domain.aggregate.Note;
import com.group01.library.domain.repository.NoteRepository;
import com.group01.library.domain.vo.LibraryStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UpdateNoteUseCase {
    private final NoteRepository repository;

    @Transactional
    public NoteResult execute(UUID userId, UUID noteId, String title, String body, LibraryStatus status) {
        Note note = ApplicationSupport.required(repository.findByIdAndUserId(noteId, userId));
        note.update(title, body, status);
        return NoteResult.from(repository.save(note));
    }
}
