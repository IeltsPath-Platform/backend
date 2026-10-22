package com.ieltspath.library.application.usecase;

import com.ieltspath.library.application.ApplicationSupport;
import com.ieltspath.library.application.port.FlashcardImageStorage;
import com.ieltspath.library.application.service.FlashcardImageUsageService;
import com.ieltspath.library.domain.aggregate.FlashcardDeck;
import com.ieltspath.library.domain.repository.FlashcardDeckItemRepository;
import com.ieltspath.library.domain.repository.FlashcardDeckRepository;
import com.ieltspath.library.domain.vo.LibraryStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeleteFlashcardDeckUseCase {
    private final FlashcardDeckRepository repository;
    private final FlashcardDeckItemRepository items;
    private final FlashcardImageStorage storage;
    private final FlashcardImageUsageService imageUsage;
    private final TransactionTemplate transactionTemplate;

    public void execute(UUID userId, UUID deckId) {
        FlashcardDeck deck = ApplicationSupport.required(repository.findByIdAndUserId(deckId, userId));
        String oldKey = deck.getCoverImageKey();

        transactionTemplate.executeWithoutResult(status -> {
            deck.update(deck.getName(), deck.getDescription(), LibraryStatus.DELETED, deck.getCoverImageKey());
            repository.save(deck);
            items.deleteByDeckId(deckId);
        });

        if (oldKey != null && !imageUsage.isStillReferenced(oldKey)) {
            storage.deleteAfterCommit(oldKey);
        }
    }
}
