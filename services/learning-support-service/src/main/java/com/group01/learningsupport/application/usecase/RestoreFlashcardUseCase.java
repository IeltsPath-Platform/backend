package com.group01.learningsupport.application.usecase;

import com.group01.learningsupport.application.ApplicationSupport;
import com.group01.learningsupport.application.result.FlashcardResult;
import com.group01.learningsupport.domain.repository.FlashcardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RestoreFlashcardUseCase {
    private final FlashcardRepository repository;

    @Transactional
    public FlashcardResult execute(UUID userId, UUID flashcardId) {
        var card = ApplicationSupport.required(repository.findByIdAndUserId(flashcardId, userId));
        card.restore();
        return FlashcardResult.from(repository.save(card));
    }
}
