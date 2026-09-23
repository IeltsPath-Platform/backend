package com.group01.learningsupport.domain.repository;

import com.group01.learningsupport.domain.aggregate.Flashcard;
import com.group01.learningsupport.domain.vo.LibraryStatus;
import com.group01.learningsupport.domain.vo.OwnedPage;

import java.util.Optional;
import java.util.UUID;

public interface FlashcardRepository {
    Flashcard save(Flashcard flashcard);

    Optional<Flashcard> findByIdAndUserId(UUID id, UUID userId);

    OwnedPage<Flashcard> findByUserIdAndStatus(UUID userId, LibraryStatus status, int page, int size);
}
