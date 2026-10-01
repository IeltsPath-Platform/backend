package com.group01.learningsupport.domain.repository;

import com.group01.learningsupport.domain.aggregate.FlashcardDeck;
import com.group01.learningsupport.domain.vo.LibraryStatus;
import com.group01.learningsupport.domain.vo.OwnedPage;

import java.util.Optional;
import java.util.UUID;

public interface FlashcardDeckRepository {
    FlashcardDeck save(FlashcardDeck deck);

    Optional<FlashcardDeck> findByIdAndUserId(UUID id, UUID userId);

    OwnedPage<FlashcardDeck> findByUserIdAndStatus(UUID userId, LibraryStatus status, int page, int size);
}
