package com.ieltspath.library.application.usecase;

import com.ieltspath.library.application.ApplicationSupport;
import com.ieltspath.library.application.result.NoteResult;
import com.ieltspath.library.domain.aggregate.Note;
import com.ieltspath.library.domain.repository.NoteRepository;
import com.ieltspath.library.domain.vo.LibraryStatus;
import com.ieltspath.library.domain.vo.NoteSourceType;
import com.ieltspath.library.domain.exception.InvalidDataException;
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
        if (note.getSourceType() == NoteSourceType.VOCABULARY_SENSE && (body == null || body.length() > 300)) {
            throw new InvalidDataException("Vocabulary note body must be at most 300 characters");
        }
        note.update(title, body, status);
        return NoteResult.from(repository.save(note));
    }
}
