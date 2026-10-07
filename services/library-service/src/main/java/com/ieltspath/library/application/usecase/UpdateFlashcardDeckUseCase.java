package com.ieltspath.library.application.usecase;

import com.ieltspath.library.application.ApplicationSupport;
import com.ieltspath.library.application.result.FlashcardDeckResult;
import com.ieltspath.library.domain.aggregate.FlashcardDeck;
import com.ieltspath.library.domain.repository.FlashcardDeckItemRepository;
import com.ieltspath.library.domain.repository.FlashcardDeckRepository;
import com.ieltspath.library.domain.vo.LibraryStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UpdateFlashcardDeckUseCase {
    private final FlashcardDeckRepository repository;
    private final FlashcardDeckItemRepository items;

    @Transactional
    public FlashcardDeckResult execute(UUID userId, UUID deckId, String name, String description, LibraryStatus status) {
        FlashcardDeck deck = ApplicationSupport.required(repository.findByIdAndUserId(deckId, userId));
        deck.update(name, description, status);
        if (status != LibraryStatus.ACTIVE) {
            items.deleteByDeckId(deckId);
        }
        return FlashcardDeckResult.from(repository.save(deck));
    }
}
