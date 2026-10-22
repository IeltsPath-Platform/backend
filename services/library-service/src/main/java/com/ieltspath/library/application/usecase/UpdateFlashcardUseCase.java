package com.ieltspath.library.application.usecase;

import com.ieltspath.commonstorage.key.ObjectKeys;
import com.ieltspath.library.application.ApplicationSupport;
import com.ieltspath.library.application.port.FlashcardImageStorage;
import com.ieltspath.library.application.result.FlashcardResult;
import com.ieltspath.library.domain.aggregate.Flashcard;
import com.ieltspath.library.domain.exception.InvalidDataException;
import com.ieltspath.library.domain.repository.FlashcardDeckItemRepository;
import com.ieltspath.library.domain.repository.FlashcardRepository;
import com.ieltspath.library.domain.vo.FlashcardSourceType;
import com.ieltspath.library.domain.vo.LibraryStatus;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UpdateFlashcardUseCase {
    private static final Logger log = LoggerFactory.getLogger(UpdateFlashcardUseCase.class);

    private final FlashcardRepository repository;
    private final FlashcardDeckItemRepository items;
    private final FlashcardImageStorage storage;

    @Transactional
    public FlashcardResult execute(
            UUID userId,
            UUID flashcardId,
            FlashcardSourceType sourceType,
            UUID vocabularySenseId,
            UUID sourceReferenceId,
            String highlightedText,
            String front,
            String back,
            LibraryStatus status,
            String imageKey,
            String exampleSentence
    ) {
        Flashcard card = ApplicationSupport.required(repository.findByIdAndUserId(flashcardId, userId));
        String oldKey = card.getImageKey();
        String newKey = imageKey != null && !imageKey.isBlank() ? imageKey.trim() : null;

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

        card.update(sourceType, vocabularySenseId, sourceReferenceId, highlightedText, front, back, status, newKey, exampleSentence);
        if (status != LibraryStatus.ACTIVE) {
            items.deleteByFlashcardId(flashcardId);
        }
        Flashcard saved = repository.save(card);

        if (oldKey != null && (!Objects.equals(oldKey, newKey) || status == LibraryStatus.DELETED)) {
            deleteAfterCommit(oldKey);
        }

        return FlashcardResult.from(saved, storage.publicUrl(saved.getImageKey()));
    }

    @Transactional
    public FlashcardResult execute(
            UUID userId,
            UUID flashcardId,
            FlashcardSourceType sourceType,
            UUID vocabularySenseId,
            UUID sourceReferenceId,
            String highlightedText,
            String front,
            String back,
            LibraryStatus status
    ) {
        Flashcard existing = ApplicationSupport.required(repository.findByIdAndUserId(flashcardId, userId));
        return execute(userId, flashcardId, sourceType, vocabularySenseId, sourceReferenceId, highlightedText,
                front, back, status, existing.getImageKey(), existing.getExampleSentence());
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
                        log.error("Failed to delete old image after commit: {}", e.getClass().getSimpleName());
                    }
                }
            });
        } else {
            try {
                storage.delete(key);
            } catch (Exception e) {
                log.error("Failed to delete old image: {}", e.getClass().getSimpleName());
            }
        }
    }
}
