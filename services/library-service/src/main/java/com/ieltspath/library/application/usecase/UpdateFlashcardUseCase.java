package com.ieltspath.library.application.usecase;

import com.ieltspath.commonstorage.key.ObjectKeys;
import com.ieltspath.library.application.ApplicationSupport;
import com.ieltspath.library.application.port.FlashcardImageStorage;
import com.ieltspath.library.application.result.FlashcardResult;
import com.ieltspath.library.application.service.FlashcardImageUsageService;
import com.ieltspath.library.domain.aggregate.Flashcard;
import com.ieltspath.library.domain.exception.InvalidDataException;
import com.ieltspath.library.domain.repository.FlashcardDeckItemRepository;
import com.ieltspath.library.domain.repository.FlashcardRepository;
import com.ieltspath.library.domain.vo.FlashcardSourceType;
import com.ieltspath.library.domain.vo.LibraryStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UpdateFlashcardUseCase {
    private final FlashcardRepository repository;
    private final FlashcardDeckItemRepository items;
    private final FlashcardImageStorage storage;
    private final FlashcardImageUsageService imageUsage;
    private final TransactionTemplate transactionTemplate;

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
            boolean imageKeySpecified,
            String imageKey,
            String exampleSentence
    ) {
        Flashcard card = ApplicationSupport.required(repository.findByIdAndUserId(flashcardId, userId));
        String oldKey = card.getImageKey();

        // If client did not specify imageKey in the request body, preserve the existing image!
        String newKey = imageKeySpecified
                ? (imageKey != null && !imageKey.isBlank() ? imageKey.trim() : null)
                : oldKey;

        // Perform HTTP check to Supabase storage OUTSIDE the database transaction
        if (newKey != null && !newKey.equals(oldKey)) {
            try {
                ObjectKeys.requireOwnedBy(newKey, "images", userId);
            } catch (Exception e) {
                throw new InvalidDataException("INVALID_IMAGE_KEY");
            }
            if (!storage.exists(newKey)) {
                throw new InvalidDataException("INVALID_IMAGE_KEY");
            }
            if (imageUsage.isImageKeyInUseByOther(newKey, flashcardId, null)) {
                throw new InvalidDataException("INVALID_IMAGE_KEY");
            }
        }

        Flashcard saved = transactionTemplate.execute(txStatus -> {
            card.update(sourceType, vocabularySenseId, sourceReferenceId, highlightedText, front, back, status, newKey, exampleSentence);
            if (status != LibraryStatus.ACTIVE) {
                items.deleteByFlashcardId(flashcardId);
            }
            return repository.save(card);
        });

        if (oldKey != null && (!Objects.equals(oldKey, newKey) || status == LibraryStatus.DELETED)) {
            if (!imageUsage.isStillReferenced(oldKey)) {
                storage.deleteAfterCommit(oldKey);
            }
        }

        return FlashcardResult.from(saved, storage.publicUrl(saved.getImageKey()));
    }

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
        return execute(userId, flashcardId, sourceType, vocabularySenseId, sourceReferenceId,
                highlightedText, front, back, status, true, imageKey, exampleSentence);
    }

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
                front, back, status, false, existing.getImageKey(), existing.getExampleSentence());
    }
}
