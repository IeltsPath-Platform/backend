package com.ieltspath.library.application.usecase;

import com.ieltspath.library.application.ApplicationSupport;
import com.ieltspath.library.application.port.FlashcardImageStorage;
import com.ieltspath.library.application.result.FlashcardDeckResult;
import com.ieltspath.library.domain.aggregate.FlashcardDeck;
import com.ieltspath.library.domain.repository.FlashcardDeckItemRepository;
import com.ieltspath.library.domain.repository.FlashcardDeckRepository;
import com.ieltspath.library.domain.vo.DeckCardCounts;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetFlashcardDeckUseCase {
    private final FlashcardDeckRepository repository;
    private final FlashcardDeckItemRepository itemRepository;
    private final FlashcardImageStorage storage;

    @Transactional(readOnly = true)
    public FlashcardDeckResult execute(UUID userId, UUID deckId) {
        FlashcardDeck deck = ApplicationSupport.required(repository.findByIdAndUserId(deckId, userId));
        Map<UUID, DeckCardCounts> countsMap = itemRepository.countCardsAndDueByDeckIds(List.of(deckId), userId, Instant.now());
        DeckCardCounts counts = countsMap.getOrDefault(deckId, DeckCardCounts.ZERO);
        String coverImageUrl = storage.publicUrl(deck.getCoverImageKey());
        return FlashcardDeckResult.from(deck, coverImageUrl, counts.cardCount(), counts.dueCount());
    }
}
