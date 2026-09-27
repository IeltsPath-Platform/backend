package com.group01.learningsupport.application.usecase;

import com.group01.learningsupport.application.ApplicationSupport;
import com.group01.learningsupport.application.result.FlashcardDeckResult;
import com.group01.learningsupport.domain.repository.FlashcardDeckRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RestoreFlashcardDeckUseCase {
    private final FlashcardDeckRepository repository;

    @Transactional
    public FlashcardDeckResult execute(UUID userId, UUID deckId) {
        var deck = ApplicationSupport.required(repository.findByIdAndUserId(deckId, userId));
        deck.restore();
        return FlashcardDeckResult.from(repository.save(deck));
    }
}
