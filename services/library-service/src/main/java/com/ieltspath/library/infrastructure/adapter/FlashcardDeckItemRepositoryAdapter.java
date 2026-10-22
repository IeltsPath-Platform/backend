package com.ieltspath.library.infrastructure.adapter;

import com.ieltspath.library.domain.aggregate.DeckItem;
import com.ieltspath.library.domain.exception.ConflictException;
import com.ieltspath.library.domain.repository.FlashcardDeckItemRepository;
import com.ieltspath.library.domain.vo.DeckCardCounts;
import com.ieltspath.library.domain.vo.OwnedPage;
import com.ieltspath.library.infrastructure.persistence.PersistenceExceptions;
import com.ieltspath.library.infrastructure.persistence.entity.FlashcardDeckItemJpaEntity;
import com.ieltspath.library.infrastructure.persistence.entity.FlashcardDeckItemKey;
import com.ieltspath.library.infrastructure.persistence.repository.FlashcardDeckItemJpaRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class FlashcardDeckItemRepositoryAdapter implements FlashcardDeckItemRepository {
    private final FlashcardDeckItemJpaRepository repository;
    private final EntityManager entityManager;

    @Override
    public void add(UUID deckId, UUID flashcardId, Integer sortOrder) {
        FlashcardDeckItemKey key = new FlashcardDeckItemKey(deckId, flashcardId);
        if (repository.existsById(key)) {
            throw new ConflictException();
        }
        FlashcardDeckItemJpaEntity entity = new FlashcardDeckItemJpaEntity();
        entity.setDeckId(deckId);
        entity.setFlashcardId(flashcardId);
        entity.setSortOrder(sortOrder);
        try {
            entityManager.persist(entity);
            entityManager.flush();
        } catch (RuntimeException exception) {
            throw PersistenceExceptions.translate(exception);
        }
    }

    @Override
    public void delete(UUID deckId, UUID flashcardId) {
        repository.deleteById(new FlashcardDeckItemKey(deckId, flashcardId));
    }

    @Override
    public void deleteByDeckId(UUID deckId) {
        repository.deleteByDeckId(deckId);
    }

    @Override
    public void deleteByFlashcardId(UUID flashcardId) {
        repository.deleteByFlashcardId(flashcardId);
    }

    @Override
    public OwnedPage<DeckItem> findActive(UUID deckId, UUID userId, int page, int size) {
        var result = repository.findActive(deckId, userId, PageRequest.of(page, size));
        return new OwnedPage<>(result.getContent(), result.getTotalElements());
    }

    @Override
    public Map<UUID, DeckCardCounts> countCardsAndDueByDeckIds(Collection<UUID> deckIds, UUID userId, Instant now) {
        if (deckIds == null || deckIds.isEmpty()) {
            return Map.of();
        }
        var list = repository.countCardsAndDueByDeckIds(deckIds, userId, now);
        Map<UUID, DeckCardCounts> map = new HashMap<>();
        for (var p : list) {
            map.put(p.getDeckId(), new DeckCardCounts(p.getCardCount(), p.getDueCount()));
        }
        return map;
    }
}
