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
public class AddFlashcardToDeckUseCase {
    private final FlashcardDeckRepository decks;
    private final FlashcardRepository cards;
    private final FlashcardDeckItemRepository items;

    @Transactional
    public void execute(UUID userId, UUID deckId, UUID flashcardId, Integer sortOrder) {
        FlashcardDeck deck = ApplicationSupport.required(decks.findByIdAndUserId(deckId, userId));
        Flashcard card = ApplicationSupport.required(cards.findByIdAndUserId(flashcardId, userId));
        if (deck.getStatus() != LibraryStatus.ACTIVE || card.getStatus() != LibraryStatus.ACTIVE) {
            throw new ResourceNotFoundException();
        }
        items.add(deckId, flashcardId, sortOrder);
    }
}
