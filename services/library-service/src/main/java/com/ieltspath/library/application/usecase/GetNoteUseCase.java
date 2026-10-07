package com.ieltspath.library.application.usecase;

import com.ieltspath.library.application.ApplicationSupport;
import com.ieltspath.library.application.result.NoteResult;
import com.ieltspath.library.domain.repository.NoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetNoteUseCase {
    private final NoteRepository repository;

    @Transactional(readOnly = true)
    public NoteResult execute(UUID userId, UUID noteId) {
        return NoteResult.from(ApplicationSupport.required(repository.findByIdAndUserId(noteId, userId)));
    }
}
