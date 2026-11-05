package com.ieltspath.library.domain.repository;

import com.ieltspath.library.domain.aggregate.Flashcard;
import com.ieltspath.library.domain.vo.LibraryStatus;
import com.ieltspath.library.domain.vo.OwnedPage;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface FlashcardRepository {
    Flashcard save(Flashcard flashcard);

    Optional<Flashcard> findByIdAndUserId(UUID id, UUID userId);

    Optional<Flashcard> findForReviewByIdAndUserId(UUID id, UUID userId);

    OwnedPage<Flashcard> findByUserIdAndStatus(UUID userId, LibraryStatus status, int page, int size);

    OwnedPage<Flashcard> findDueCards(UUID userId, UUID deckId, Instant now, int page, int limit);

    /** The learner's not-deleted flashcard saved from this practice question, if any. */
    Optional<Flashcard> findLivePracticeQuestionCard(UUID userId, UUID practiceQuestionId);

    default Optional<Flashcard> findLiveVocabularySenseCard(UUID userId, UUID vocabularySenseId) { return Optional.empty(); }

    default long countByImageKeyAndStatusNot(String imageKey, LibraryStatus status) {
        return 0L;
    }

    default boolean existsByImageKeyAndIdNotAndStatusNot(String imageKey, UUID id, LibraryStatus status) {
        return false;
    }
}
