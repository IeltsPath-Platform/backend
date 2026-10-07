package com.ieltspath.library.application.usecase;

import com.ieltspath.library.application.ApplicationSupport;
import com.ieltspath.library.application.result.FlashcardResult;
import com.ieltspath.library.domain.repository.FlashcardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetFlashcardUseCase {
    private final FlashcardRepository repository;

    @Transactional(readOnly = true)
    public FlashcardResult execute(UUID userId, UUID flashcardId) {
        return FlashcardResult.from(ApplicationSupport.required(repository.findByIdAndUserId(flashcardId, userId)));
    }
}
