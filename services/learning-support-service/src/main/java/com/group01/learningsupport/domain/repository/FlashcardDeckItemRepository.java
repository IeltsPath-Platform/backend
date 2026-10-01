package com.group01.learningsupport.domain.repository;

import com.group01.learningsupport.domain.aggregate.DeckItem;
import com.group01.learningsupport.domain.vo.OwnedPage;

import java.util.UUID;

public interface FlashcardDeckItemRepository {
    void add(UUID deckId, UUID flashcardId, Integer sortOrder);

    void delete(UUID deckId, UUID flashcardId);

    void deleteByDeckId(UUID deckId);

    void deleteByFlashcardId(UUID flashcardId);

    OwnedPage<DeckItem> findActive(UUID deckId, UUID userId, int page, int size);
}
