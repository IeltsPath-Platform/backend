package com.group01.learningsupport.infrastructure.persistence.repository;

import com.group01.learningsupport.domain.aggregate.DeckItem;
import com.group01.learningsupport.infrastructure.persistence.entity.FlashcardDeckItemJpaEntity;
import com.group01.learningsupport.infrastructure.persistence.entity.FlashcardDeckItemKey;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface FlashcardDeckItemJpaRepository extends JpaRepository<FlashcardDeckItemJpaEntity, FlashcardDeckItemKey> {
    void deleteByDeckId(UUID deckId);

    void deleteByFlashcardId(UUID flashcardId);

    @Query(
            value = """
                    SELECT new com.group01.learningsupport.domain.aggregate.DeckItem(
                        i.deckId, i.flashcardId, i.sortOrder, i.addedAt, c.front, c.back, c.sourceType, c.status
                    )
                    FROM FlashcardDeckItemJpaEntity i, FlashcardJpaEntity c, FlashcardDeckJpaEntity d
                    WHERE i.deckId = :deckId
                      AND c.id = i.flashcardId
                      AND d.id = i.deckId
                      AND d.userId = :userId
                      AND c.userId = :userId
                      AND c.status = com.group01.learningsupport.domain.vo.LibraryStatus.ACTIVE
                    ORDER BY i.sortOrder ASC NULLS LAST, i.flashcardId ASC
                    """,
            countQuery = """
                    SELECT count(i)
                    FROM FlashcardDeckItemJpaEntity i, FlashcardJpaEntity c, FlashcardDeckJpaEntity d
                    WHERE i.deckId = :deckId
                      AND c.id = i.flashcardId
                      AND d.id = i.deckId
                      AND d.userId = :userId
                      AND c.userId = :userId
                      AND c.status = com.group01.learningsupport.domain.vo.LibraryStatus.ACTIVE
                    """
    )
    Page<DeckItem> findActive(@Param("deckId") UUID deckId, @Param("userId") UUID userId, Pageable pageable);
}
