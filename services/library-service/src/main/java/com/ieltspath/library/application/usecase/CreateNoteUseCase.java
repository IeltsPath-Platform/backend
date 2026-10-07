package com.ieltspath.library.application.usecase;

import com.ieltspath.library.application.result.NoteResult;
import com.ieltspath.library.domain.aggregate.Note;
import com.ieltspath.library.domain.repository.NoteRepository;
import com.ieltspath.library.domain.vo.NoteSourceType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CreateNoteUseCase {
    private final NoteRepository repository;

    @Transactional
    public NoteResult execute(UUID userId, String title, String body) {
        return NoteResult.from(repository.save(Note.create(userId, title, body)));
    }

    @Transactional
    public NoteResult execute(UUID userId, String title, String body,
                              NoteSourceType sourceType, UUID sourceReferenceId) {
        return NoteResult.from(repository.save(
                Note.create(userId, title, body, sourceType, sourceReferenceId)));
    }
}
