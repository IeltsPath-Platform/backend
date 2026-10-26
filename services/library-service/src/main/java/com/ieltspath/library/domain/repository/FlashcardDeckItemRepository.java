package com.ieltspath.library.domain.repository;

import com.ieltspath.library.domain.aggregate.DeckItem;
import com.ieltspath.library.domain.vo.DeckCardCounts;
import com.ieltspath.library.domain.vo.OwnedPage;

import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

public interface FlashcardDeckItemRepository {
    void add(UUID deckId, UUID flashcardId, Integer sortOrder);

    void delete(UUID deckId, UUID flashcardId);

    void deleteByDeckId(UUID deckId);

    void deleteByFlashcardId(UUID flashcardId);

    OwnedPage<DeckItem> findActive(UUID deckId, UUID userId, int page, int size);

    Map<UUID, DeckCardCounts> countCardsAndDueByDeckIds(Collection<UUID> deckIds, UUID userId, Instant now);
}
