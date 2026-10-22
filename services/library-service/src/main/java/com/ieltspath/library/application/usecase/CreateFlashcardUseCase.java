package com.ieltspath.library.application.usecase;

import com.ieltspath.commonstorage.key.ObjectKeys;
import com.ieltspath.library.application.port.FlashcardImageStorage;
import com.ieltspath.library.application.result.FlashcardResult;
import com.ieltspath.library.domain.aggregate.Flashcard;
import com.ieltspath.library.domain.exception.InvalidDataException;
import com.ieltspath.library.domain.repository.FlashcardRepository;
import com.ieltspath.library.domain.vo.FlashcardSourceType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CreateFlashcardUseCase {
    private final FlashcardRepository repository;
    private final FlashcardImageStorage storage;

    @Transactional
    public FlashcardResult execute(
            UUID userId,
            FlashcardSourceType sourceType,
            UUID vocabularySenseId,
            UUID sourceReferenceId,
            String highlightedText,
            String front,
            String back,
            String imageKey,
            String exampleSentence
    ) {
        String key = imageKey != null && !imageKey.isBlank() ? imageKey.trim() : null;
        if (key != null) {
            try {
                ObjectKeys.requireOwnedBy(key, "images", userId);
            } catch (Exception e) {
                throw new InvalidDataException("INVALID_IMAGE_KEY");
            }
            if (!storage.exists(key)) {
                throw new InvalidDataException("INVALID_IMAGE_KEY");
            }
        }
        Flashcard card = repository.save(Flashcard.create(
                userId, sourceType, vocabularySenseId, sourceReferenceId, highlightedText, front, back, key, exampleSentence
        ));
        return FlashcardResult.from(card, storage.publicUrl(card.getImageKey()));
    }

    @Transactional
    public FlashcardResult execute(
            UUID userId,
            FlashcardSourceType sourceType,
            UUID vocabularySenseId,
            UUID sourceReferenceId,
            String highlightedText,
            String front,
            String back
    ) {
        return execute(userId, sourceType, vocabularySenseId, sourceReferenceId, highlightedText, front, back, null, null);
    }
}
