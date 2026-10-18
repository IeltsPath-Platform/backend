package com.ieltspath.library.infrastructure.persistence.repository;

import com.ieltspath.library.domain.vo.FlashcardSourceType;
import com.ieltspath.library.domain.vo.LibraryStatus;
import com.ieltspath.library.infrastructure.persistence.entity.FlashcardJpaEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface FlashcardJpaRepository extends JpaRepository<FlashcardJpaEntity, UUID> {
    Optional<FlashcardJpaEntity> findByIdAndUserId(UUID id, UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select card from FlashcardJpaEntity card where card.id = :id and card.userId = :userId")
    Optional<FlashcardJpaEntity> findForReviewByIdAndUserId(@Param("id") UUID id, @Param("userId") UUID userId);

    Page<FlashcardJpaEntity> findByUserIdAndStatus(UUID userId, LibraryStatus status, Pageable pageable);

    @Query(value = """
            select card from FlashcardJpaEntity card
            where card.userId = :userId
              and card.status = :active
              and (card.nextReviewAt is null or card.nextReviewAt <= :now)
              and (:deckId is null or exists (
                  select item.flashcardId from FlashcardDeckItemJpaEntity item, FlashcardDeckJpaEntity deck
                  where item.deckId = deck.id and item.flashcardId = card.id
                    and deck.id = :deckId and deck.userId = :userId and deck.status = :active
              ))
            order by case when card.nextReviewAt is null then 0 else 1 end,
                     card.nextReviewAt asc, card.id asc
            """,
            countQuery = """
            select count(card) from FlashcardJpaEntity card
            where card.userId = :userId
              and card.status = :active
              and (card.nextReviewAt is null or card.nextReviewAt <= :now)
              and (:deckId is null or exists (
                  select item.flashcardId from FlashcardDeckItemJpaEntity item, FlashcardDeckJpaEntity deck
                  where item.deckId = deck.id and item.flashcardId = card.id
                    and deck.id = :deckId and deck.userId = :userId and deck.status = :active
              ))
            """)
    Page<FlashcardJpaEntity> findDueCards(@Param("userId") UUID userId, @Param("deckId") UUID deckId,
                                          @Param("now") Instant now,
                                          @Param("active") LibraryStatus active, Pageable pageable);

    Optional<FlashcardJpaEntity> findFirstByUserIdAndSourceTypeAndSourceReferenceIdAndStatusNot(
            UUID userId, FlashcardSourceType sourceType, UUID sourceReferenceId, LibraryStatus status);
}
