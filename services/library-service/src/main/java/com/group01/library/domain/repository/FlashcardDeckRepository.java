package com.group01.library.domain.repository;

import com.group01.library.domain.aggregate.FlashcardDeck;
import com.group01.library.domain.vo.LibraryStatus;
import com.group01.library.domain.vo.OwnedPage;

import java.util.Optional;
import java.util.UUID;

public interface FlashcardDeckRepository {
    FlashcardDeck save(FlashcardDeck deck);

    Optional<FlashcardDeck> findByIdAndUserId(UUID id, UUID userId);

    OwnedPage<FlashcardDeck> findByUserIdAndStatus(UUID userId, LibraryStatus status, int page, int size);
}
