package com.ieltspath.library.application.service;

import com.ieltspath.library.domain.repository.FlashcardDeckRepository;
import com.ieltspath.library.domain.repository.FlashcardRepository;
import com.ieltspath.library.domain.vo.LibraryStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FlashcardImageUsageService {
    private final FlashcardRepository flashcardRepository;
    private final FlashcardDeckRepository deckRepository;

    /**
     * Checks whether an image key is already assigned to another active card or deck.
     */
    public boolean isImageKeyInUseByOther(String key, UUID currentCardId, UUID currentDeckId) {
        if (key == null || key.isBlank()) {
            return false;
        }
        boolean inCards = currentCardId != null
                ? flashcardRepository.existsByImageKeyAndIdNotAndStatusNot(key, currentCardId, LibraryStatus.DELETED)
                : flashcardRepository.countByImageKeyAndStatusNot(key, LibraryStatus.DELETED) > 0;
        if (inCards) {
            return true;
        }
        return currentDeckId != null
                ? deckRepository.existsByCoverImageKeyAndIdNotAndStatusNot(key, currentDeckId, LibraryStatus.DELETED)
                : deckRepository.countByCoverImageKeyAndStatusNot(key, LibraryStatus.DELETED) > 0;
    }

    /**
     * Checks if any active card or deck is still referencing this key.
     */
    public boolean isStillReferenced(String key) {
        if (key == null || key.isBlank()) {
            return false;
        }
        return flashcardRepository.countByImageKeyAndStatusNot(key, LibraryStatus.DELETED) > 0
                || deckRepository.countByCoverImageKeyAndStatusNot(key, LibraryStatus.DELETED) > 0;
    }
}

