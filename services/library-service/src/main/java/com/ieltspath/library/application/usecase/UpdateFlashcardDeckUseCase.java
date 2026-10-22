package com.ieltspath.library.application.usecase;

import com.ieltspath.commonstorage.key.ObjectKeys;
import com.ieltspath.library.application.ApplicationSupport;
import com.ieltspath.library.application.port.FlashcardImageStorage;
import com.ieltspath.library.application.result.FlashcardDeckResult;
import com.ieltspath.library.application.service.FlashcardImageUsageService;
import com.ieltspath.library.domain.aggregate.FlashcardDeck;
import com.ieltspath.library.domain.exception.InvalidDataException;
import com.ieltspath.library.domain.repository.FlashcardDeckItemRepository;
import com.ieltspath.library.domain.repository.FlashcardDeckRepository;
import com.ieltspath.library.domain.vo.DeckCardCounts;
import com.ieltspath.library.domain.vo.LibraryStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UpdateFlashcardDeckUseCase {
    private final FlashcardDeckRepository repository;
    private final FlashcardDeckItemRepository items;
    private final FlashcardImageStorage storage;
    private final FlashcardImageUsageService imageUsage;
    private final TransactionTemplate transactionTemplate;

    public FlashcardDeckResult execute(
            UUID userId,
            UUID deckId,
            String name,
            String description,
            LibraryStatus status,
            boolean coverImageKeySpecified,
            String coverImageKey
    ) {
        FlashcardDeck deck = ApplicationSupport.required(repository.findByIdAndUserId(deckId, userId));
        String oldKey = deck.getCoverImageKey();

        // If client did not specify coverImageKey in the request body, preserve the existing cover image!
        String newKey = coverImageKeySpecified
                ? (coverImageKey != null && !coverImageKey.isBlank() ? coverImageKey.trim() : null)
                : oldKey;

        // Storage verification outside DB transaction
        if (newKey != null && !newKey.equals(oldKey)) {
            try {
                ObjectKeys.requireOwnedBy(newKey, "images", userId);
            } catch (Exception e) {
                throw new InvalidDataException("INVALID_IMAGE_KEY");
            }
            if (!storage.exists(newKey)) {
                throw new InvalidDataException("INVALID_IMAGE_KEY");
            }
            if (imageUsage.isImageKeyInUseByOther(newKey, null, deckId)) {
                throw new InvalidDataException("INVALID_IMAGE_KEY");
            }
        }

        FlashcardDeck saved = transactionTemplate.execute(txStatus -> {
            deck.update(name, description, status, newKey);
            if (status != LibraryStatus.ACTIVE) {
                items.deleteByDeckId(deckId);
            }
            return repository.save(deck);
        });

        if (oldKey != null && (!Objects.equals(oldKey, newKey) || status == LibraryStatus.DELETED)) {
            if (!imageUsage.isStillReferenced(oldKey)) {
                storage.deleteAfterCommit(oldKey);
            }
        }

        var countsMap = items.countCardsAndDueByDeckIds(List.of(deckId), userId, Instant.now());
        DeckCardCounts counts = countsMap.getOrDefault(deckId, DeckCardCounts.ZERO);

        return FlashcardDeckResult.from(saved, storage.publicUrl(saved.getCoverImageKey()),
                counts.cardCount(), counts.dueCount());
    }

    public FlashcardDeckResult execute(
            UUID userId,
            UUID deckId,
            String name,
            String description,
            LibraryStatus status,
            String coverImageKey
    ) {
        return execute(userId, deckId, name, description, status, true, coverImageKey);
    }

    public FlashcardDeckResult execute(
            UUID userId,
            UUID deckId,
            String name,
            String description,
            LibraryStatus status
    ) {
        return execute(userId, deckId, name, description, status, false, null);
    }
}
