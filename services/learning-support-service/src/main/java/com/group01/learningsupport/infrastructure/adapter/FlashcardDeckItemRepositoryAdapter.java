package com.group01.learningsupport.infrastructure.adapter;

import com.group01.learningsupport.domain.aggregate.DeckItem;
import com.group01.learningsupport.domain.exception.ConflictException;
import com.group01.learningsupport.domain.repository.FlashcardDeckItemRepository;
import com.group01.learningsupport.domain.vo.OwnedPage;
import com.group01.learningsupport.infrastructure.persistence.PersistenceExceptions;
import com.group01.learningsupport.infrastructure.persistence.entity.FlashcardDeckItemJpaEntity;
import com.group01.learningsupport.infrastructure.persistence.entity.FlashcardDeckItemKey;
import com.group01.learningsupport.infrastructure.persistence.repository.FlashcardDeckItemJpaRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

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
}
