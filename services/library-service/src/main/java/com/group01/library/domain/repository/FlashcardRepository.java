package com.group01.library.domain.repository;

import com.group01.library.domain.aggregate.Flashcard;
import com.group01.library.domain.vo.LibraryStatus;
import com.group01.library.domain.vo.OwnedPage;

import java.util.Optional;
import java.util.UUID;

public interface FlashcardRepository {
    Flashcard save(Flashcard flashcard);

    Optional<Flashcard> findByIdAndUserId(UUID id, UUID userId);

    OwnedPage<Flashcard> findByUserIdAndStatus(UUID userId, LibraryStatus status, int page, int size);

    /** The learner's not-deleted flashcard saved from this practice question, if any. */
    Optional<Flashcard> findLivePracticeQuestionCard(UUID userId, UUID practiceQuestionId);
}
