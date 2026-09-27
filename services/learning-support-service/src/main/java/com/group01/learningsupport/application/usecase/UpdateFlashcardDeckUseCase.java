package com.group01.learningsupport.application.usecase;

import com.group01.learningsupport.application.ApplicationSupport;
import com.group01.learningsupport.application.result.FlashcardDeckResult;
import com.group01.learningsupport.domain.aggregate.FlashcardDeck;
import com.group01.learningsupport.domain.repository.FlashcardDeckRepository;
import com.group01.learningsupport.domain.vo.LibraryStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UpdateFlashcardDeckUseCase {
    private final FlashcardDeckRepository repository;

    @Transactional
    public FlashcardDeckResult execute(UUID userId, UUID deckId, String name, String description, LibraryStatus status) {
        FlashcardDeck deck = ApplicationSupport.required(repository.findByIdAndUserId(deckId, userId));
        deck.update(name, description, status);
        return FlashcardDeckResult.from(repository.save(deck));
    }
}
