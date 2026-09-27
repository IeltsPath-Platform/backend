package com.group01.learningsupport.application.usecase;

import com.group01.learningsupport.application.ApplicationSupport;
import com.group01.learningsupport.application.query.PageQuery;
import com.group01.learningsupport.application.result.DeckItemResult;
import com.group01.learningsupport.application.result.PageResult;
import com.group01.learningsupport.domain.repository.FlashcardDeckItemRepository;
import com.group01.learningsupport.domain.repository.FlashcardDeckRepository;
import com.group01.learningsupport.domain.exception.ResourceNotFoundException;
import com.group01.learningsupport.domain.vo.LibraryStatus;
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
        var deck = ApplicationSupport.required(decks.findByIdAndUserId(deckId, userId));
        if (deck.getStatus() != LibraryStatus.ACTIVE) {
            throw new ResourceNotFoundException();
        }
        return ApplicationSupport.page(items.findActive(deckId, userId, query.page(), query.size()), query)
                .map(DeckItemResult::from);
    }
}
