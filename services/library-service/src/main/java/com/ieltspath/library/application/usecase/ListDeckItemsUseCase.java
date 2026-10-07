package com.ieltspath.library.application.usecase;

import com.ieltspath.library.application.ApplicationSupport;
import com.ieltspath.library.application.query.PageQuery;
import com.ieltspath.library.application.result.DeckItemResult;
import com.ieltspath.library.application.result.PageResult;
import com.ieltspath.library.domain.repository.FlashcardDeckItemRepository;
import com.ieltspath.library.domain.repository.FlashcardDeckRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ListDeckItemsUseCase {
    private final FlashcardDeckRepository decks;
    private final FlashcardDeckItemRepository items;

    @Transactional(readOnly = true)
    public PageResult<DeckItemResult> execute(UUID userId, UUID deckId, PageQuery query) {
        ApplicationSupport.required(decks.findByIdAndUserId(deckId, userId));
        return ApplicationSupport.page(items.findActive(deckId, userId, query.page(), query.size()), query)
                .map(DeckItemResult::from);
    }
}
