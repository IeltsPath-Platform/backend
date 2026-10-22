package com.ieltspath.library.application.usecase;

import com.ieltspath.commonstorage.key.ObjectKeys;
import com.ieltspath.library.application.ApplicationSupport;
import com.ieltspath.library.application.port.FlashcardImageStorage;
import com.ieltspath.library.application.result.FlashcardDeckResult;
import com.ieltspath.library.domain.aggregate.FlashcardDeck;
import com.ieltspath.library.domain.exception.InvalidDataException;
import com.ieltspath.library.domain.repository.FlashcardDeckItemRepository;
import com.ieltspath.library.domain.repository.FlashcardDeckRepository;
import com.ieltspath.library.domain.vo.DeckCardCounts;
import com.ieltspath.library.domain.vo.LibraryStatus;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UpdateFlashcardDeckUseCase {
    private static final Logger log = LoggerFactory.getLogger(UpdateFlashcardDeckUseCase.class);

    private final FlashcardDeckRepository repository;
    private final FlashcardDeckItemRepository items;
    private final FlashcardImageStorage storage;

    @Transactional
    public FlashcardDeckResult execute(
            UUID userId,
            UUID deckId,
            String name,
            String description,
            LibraryStatus status,
            String coverImageKey
    ) {
        FlashcardDeck deck = ApplicationSupport.required(repository.findByIdAndUserId(deckId, userId));
        String oldKey = deck.getCoverImageKey();
        String newKey = coverImageKey != null && !coverImageKey.isBlank() ? coverImageKey.trim() : null;

        if (newKey != null && !newKey.equals(oldKey)) {
            try {
                ObjectKeys.requireOwnedBy(newKey, "images", userId);
            } catch (Exception e) {
                throw new InvalidDataException("INVALID_IMAGE_KEY");
            }
            if (!storage.exists(newKey)) {
                throw new InvalidDataException("INVALID_IMAGE_KEY");
            }
        }

        deck.update(name, description, status, newKey);
        if (status != LibraryStatus.ACTIVE) {
            items.deleteByDeckId(deckId);
        }
        FlashcardDeck saved = repository.save(deck);

        if (oldKey != null && (!Objects.equals(oldKey, newKey) || status == LibraryStatus.DELETED)) {
            deleteAfterCommit(oldKey);
        }

        var countsMap = items.countCardsAndDueByDeckIds(List.of(deckId), userId, Instant.now());
        DeckCardCounts counts = countsMap.getOrDefault(deckId, DeckCardCounts.ZERO);

        return FlashcardDeckResult.from(saved, storage.publicUrl(saved.getCoverImageKey()),
                counts.cardCount(), counts.dueCount());
    }

    @Transactional
    public FlashcardDeckResult execute(
            UUID userId,
            UUID deckId,
            String name,
            String description,
            LibraryStatus status
    ) {
        FlashcardDeck existing = ApplicationSupport.required(repository.findByIdAndUserId(deckId, userId));
        return execute(userId, deckId, name, description, status, existing.getCoverImageKey());
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
