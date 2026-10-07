package com.ieltspath.library.application.usecase;

import com.ieltspath.library.application.ApplicationSupport;
import com.ieltspath.library.application.result.FlashcardDeckResult;
import com.ieltspath.library.domain.repository.FlashcardDeckRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetFlashcardDeckUseCase {
    private final FlashcardDeckRepository repository;

    @Transactional(readOnly = true)
    public FlashcardDeckResult execute(UUID userId, UUID deckId) {
        return FlashcardDeckResult.from(ApplicationSupport.required(repository.findByIdAndUserId(deckId, userId)));
    }
}
