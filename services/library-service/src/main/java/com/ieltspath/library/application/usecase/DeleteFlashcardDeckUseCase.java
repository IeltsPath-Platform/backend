package com.ieltspath.library.application.usecase;

import com.ieltspath.library.application.ApplicationSupport;
import com.ieltspath.library.application.port.FlashcardImageStorage;
import com.ieltspath.library.domain.aggregate.FlashcardDeck;
import com.ieltspath.library.domain.repository.FlashcardDeckItemRepository;
import com.ieltspath.library.domain.repository.FlashcardDeckRepository;
import com.ieltspath.library.domain.vo.LibraryStatus;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeleteFlashcardDeckUseCase {
    private static final Logger log = LoggerFactory.getLogger(DeleteFlashcardDeckUseCase.class);

    private final FlashcardDeckRepository repository;
    private final FlashcardDeckItemRepository items;
    private final FlashcardImageStorage storage;

    @Transactional
    public void execute(UUID userId, UUID deckId) {
        FlashcardDeck deck = ApplicationSupport.required(repository.findByIdAndUserId(deckId, userId));
        String oldKey = deck.getCoverImageKey();
        deck.update(deck.getName(), deck.getDescription(), LibraryStatus.DELETED);
        repository.save(deck);
        items.deleteByDeckId(deckId);

        if (oldKey != null) {
            deleteAfterCommit(oldKey);
        }
    }

    private void deleteAfterCommit(String key) {
        if (key == null || key.isBlank()) {
            return;
        }
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        storage.delete(key);
                    } catch (Exception e) {
                        log.error("Failed to delete deck cover image after commit: {}", e.getClass().getSimpleName());
                    }
                }
            });
        } else {
            try {
                storage.delete(key);
            } catch (Exception e) {
                log.error("Failed to delete deck cover image: {}", e.getClass().getSimpleName());
            }
        }
    }
}
