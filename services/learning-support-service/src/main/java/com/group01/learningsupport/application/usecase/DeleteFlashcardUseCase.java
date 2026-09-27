package com.group01.learningsupport.application.usecase;

import com.group01.learningsupport.application.ApplicationSupport;
import com.group01.learningsupport.domain.aggregate.Flashcard;
import com.group01.learningsupport.domain.repository.FlashcardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeleteFlashcardUseCase {
    private final FlashcardRepository repository;

    @Transactional
    public void execute(UUID userId, UUID flashcardId) {
        Flashcard card = ApplicationSupport.required(repository.findAvailableByIdAndUserId(flashcardId, userId));
        card.delete();
        repository.save(card);
    }
}
