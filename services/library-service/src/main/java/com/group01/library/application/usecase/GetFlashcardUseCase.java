package com.group01.library.application.usecase;

import com.group01.library.application.ApplicationSupport;
import com.group01.library.application.result.FlashcardResult;
import com.group01.library.domain.repository.FlashcardRepository;
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
