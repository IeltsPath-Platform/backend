package com.ieltspath.library.application.usecase;

import com.ieltspath.library.application.result.FlashcardDeckResult;
import com.ieltspath.library.domain.aggregate.FlashcardDeck;
import com.ieltspath.library.domain.repository.FlashcardDeckRepository;
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
