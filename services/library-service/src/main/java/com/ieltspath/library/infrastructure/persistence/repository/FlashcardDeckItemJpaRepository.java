package com.ieltspath.library.infrastructure.persistence.repository;

import com.ieltspath.library.domain.aggregate.DeckItem;
import com.ieltspath.library.infrastructure.persistence.entity.FlashcardDeckItemJpaEntity;
import com.ieltspath.library.infrastructure.persistence.entity.FlashcardDeckItemKey;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface FlashcardDeckItemJpaRepository extends JpaRepository<FlashcardDeckItemJpaEntity, FlashcardDeckItemKey> {
    void deleteByDeckId(UUID deckId);

    void deleteByFlashcardId(UUID flashcardId);

    @Query(
            value = """
                    SELECT new com.ieltspath.library.domain.aggregate.DeckItem(
                        i.deckId, i.flashcardId, i.sortOrder, i.addedAt, c.front, c.back, c.sourceType, c.status,
                        c.imageKey, c.exampleSentence, c.leitnerBox, c.vocabularySenseId
                    )
                    FROM FlashcardDeckItemJpaEntity i, FlashcardJpaEntity c, FlashcardDeckJpaEntity d
                    WHERE i.deckId = :deckId
                      AND c.id = i.flashcardId
                      AND d.id = i.deckId
                      AND d.userId = :userId
                      AND c.userId = :userId
                      AND c.status = com.ieltspath.library.domain.vo.LibraryStatus.ACTIVE
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
                      AND c.status = com.ieltspath.library.domain.vo.LibraryStatus.ACTIVE
                    """
    )
    Page<DeckItem> findActive(@Param("deckId") UUID deckId, @Param("userId") UUID userId, Pageable pageable);

    @Query("""
            SELECT i.deckId AS deckId,
                   COUNT(c.id) AS cardCount,
                   COUNT(CASE WHEN (c.nextReviewAt IS NULL OR c.nextReviewAt <= :now) THEN 1 END) AS dueCount
            FROM FlashcardDeckItemJpaEntity i, FlashcardJpaEntity c
            WHERE i.deckId IN (:deckIds)
              AND c.id = i.flashcardId
              AND c.userId = :userId
              AND c.status = com.ieltspath.library.domain.vo.LibraryStatus.ACTIVE
            GROUP BY i.deckId
            """)
    List<DeckCardCountProjection> countCardsAndDueByDeckIds(
            @Param("deckIds") Collection<UUID> deckIds,
            @Param("userId") UUID userId,
            @Param("now") Instant now
    );
}
