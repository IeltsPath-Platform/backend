package com.group01.learningsupport.application.usecase;

import com.group01.learningsupport.application.ApplicationSupport;
import com.group01.learningsupport.application.query.PageQuery;
import com.group01.learningsupport.application.result.PageResult;
import com.group01.learningsupport.domain.aggregate.Flashcard;
import com.group01.learningsupport.domain.aggregate.FlashcardDeck;
import com.group01.learningsupport.domain.aggregate.Note;
import com.group01.learningsupport.domain.exception.ResourceNotFoundException;
import com.group01.learningsupport.domain.repository.FlashcardDeckItemRepository;
import com.group01.learningsupport.domain.repository.FlashcardDeckRepository;
import com.group01.learningsupport.domain.repository.FlashcardRepository;
import com.group01.learningsupport.domain.repository.NoteRepository;
import com.group01.learningsupport.domain.vo.FlashcardSourceType;
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
    public PageResult<com.group01.learningsupport.domain.aggregate.DeckItem> execute(UUID userId, UUID deckId, PageQuery query) {
        ApplicationSupport.required(decks.findByIdAndUserId(deckId, userId));
        return ApplicationSupport.page(items.findActive(deckId, userId, query.page(), query.size()), query);
    }
}
