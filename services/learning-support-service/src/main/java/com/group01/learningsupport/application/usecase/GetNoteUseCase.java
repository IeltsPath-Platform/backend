package com.group01.learningsupport.application.usecase;

import com.group01.learningsupport.application.ApplicationSupport;
import com.group01.learningsupport.application.result.NoteResult;
import com.group01.learningsupport.domain.repository.NoteRepository;
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
