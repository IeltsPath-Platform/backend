package com.ieltspath.library.domain.repository;

import com.ieltspath.library.domain.aggregate.FlashcardDeck;
import com.ieltspath.library.domain.vo.LibraryStatus;
import com.ieltspath.library.domain.vo.OwnedPage;

import java.util.Optional;
import java.util.UUID;

public interface FlashcardDeckRepository {
    FlashcardDeck save(FlashcardDeck deck);

    Optional<FlashcardDeck> findByIdAndUserId(UUID id, UUID userId);

    OwnedPage<FlashcardDeck> findByUserIdAndStatus(UUID userId, LibraryStatus status, int page, int size);

    default long countByCoverImageKeyAndStatusNot(String coverImageKey, LibraryStatus status) {
        return 0L;
    }

    default boolean existsByCoverImageKeyAndIdNotAndStatusNot(String coverImageKey, UUID id, LibraryStatus status) {
        return false;
    }
}
