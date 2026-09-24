package com.group01.learningsupport.application.usecase;

import com.group01.learningsupport.application.result.NoteResult;
import com.group01.learningsupport.domain.aggregate.Note;
import com.group01.learningsupport.domain.repository.NoteRepository;
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
}
