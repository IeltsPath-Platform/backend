package com.ieltspath.library.application.usecase;

import com.ieltspath.commonstorage.key.ObjectKeys;
import com.ieltspath.library.application.port.FlashcardImageStorage;
import com.ieltspath.library.application.result.FlashcardDeckResult;
import com.ieltspath.library.application.service.FlashcardImageUsageService;
import com.ieltspath.library.domain.aggregate.FlashcardDeck;
import com.ieltspath.library.domain.exception.InvalidDataException;
import com.ieltspath.library.domain.repository.FlashcardDeckRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CreateFlashcardDeckUseCase {
    private final FlashcardDeckRepository repository;
    private final FlashcardImageStorage storage;
    private final FlashcardImageUsageService imageUsage;
    private final TransactionTemplate transactionTemplate;

    public FlashcardDeckResult execute(UUID userId, String name, String description, String coverImageKey) {
        String key = coverImageKey != null && !coverImageKey.isBlank() ? coverImageKey.trim() : null;
        if (key != null) {
            try {
                ObjectKeys.requireOwnedBy(key, "images", userId);
            } catch (Exception e) {
                throw new InvalidDataException("INVALID_IMAGE_KEY");
            }
            if (!storage.exists(key)) {
                throw new InvalidDataException("INVALID_IMAGE_KEY");
            }
            if (imageUsage.isImageKeyInUseByOther(key, null, null)) {
                throw new InvalidDataException("INVALID_IMAGE_KEY");
            }
        }

        FlashcardDeck deck = transactionTemplate.execute(status ->
                repository.save(FlashcardDeck.create(userId, name, description, key))
        );
        return FlashcardDeckResult.from(deck, storage.publicUrl(deck.getCoverImageKey()), 0, 0);
    }

    public FlashcardDeckResult execute(UUID userId, String name, String description) {
        return execute(userId, name, description, null);
    }
}
