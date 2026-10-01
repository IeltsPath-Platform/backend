package com.group01.learningsupport.application.usecase;

import com.group01.learningsupport.application.result.FlashcardDeckResult;
import com.group01.learningsupport.domain.aggregate.FlashcardDeck;
import com.group01.learningsupport.domain.repository.FlashcardDeckRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CreateFlashcardDeckUseCase {
    private final FlashcardDeckRepository repository;

    @Transactional
    public FlashcardDeckResult execute(UUID userId, String name, String description) {
        return FlashcardDeckResult.from(repository.save(FlashcardDeck.create(userId, name, description)));
    }
}
