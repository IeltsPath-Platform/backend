package com.ieltspath.library.application.usecase;

import com.ieltspath.library.application.ApplicationSupport;
import com.ieltspath.library.application.port.FlashcardImageStorage;
import com.ieltspath.library.application.query.PageQuery;
import com.ieltspath.library.application.result.FlashcardDeckResult;
import com.ieltspath.library.application.result.PageResult;
import com.ieltspath.library.domain.aggregate.FlashcardDeck;
import com.ieltspath.library.domain.repository.FlashcardDeckItemRepository;
import com.ieltspath.library.domain.repository.FlashcardDeckRepository;
import com.ieltspath.library.domain.vo.DeckCardCounts;
import com.ieltspath.library.domain.vo.LibraryStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ListFlashcardDecksUseCase {
    private final FlashcardDeckRepository repository;
    private final FlashcardDeckItemRepository itemRepository;
    private final FlashcardImageStorage storage;

    @Transactional(readOnly = true)
    public PageResult<FlashcardDeckResult> execute(UUID userId, LibraryStatus status, PageQuery query) {
        var deckPage = repository.findByUserIdAndStatus(userId, status, query.page(), query.size());
        List<FlashcardDeck> decks = deckPage.items();
        List<UUID> deckIds = decks.stream().map(FlashcardDeck::getId).toList();
        Map<UUID, DeckCardCounts> countsMap = itemRepository.countCardsAndDueByDeckIds(deckIds, userId, Instant.now());

        List<FlashcardDeckResult> results = decks.stream().map(deck -> {
            DeckCardCounts counts = countsMap.getOrDefault(deck.getId(), DeckCardCounts.ZERO);
            String coverImageUrl = storage.publicUrl(deck.getCoverImageKey());
            return FlashcardDeckResult.from(deck, coverImageUrl, counts.cardCount(), counts.dueCount());
        }).toList();

        return new PageResult<>(results, query.page(), query.size(), deckPage.total());
    }
}
