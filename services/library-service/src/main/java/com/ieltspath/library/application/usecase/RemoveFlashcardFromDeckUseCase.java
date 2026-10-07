package com.ieltspath.library.application.usecase;

import com.ieltspath.library.application.ApplicationSupport;
import com.ieltspath.library.application.query.PageQuery;
import com.ieltspath.library.application.result.PageResult;
import com.ieltspath.library.domain.aggregate.Flashcard;
import com.ieltspath.library.domain.aggregate.FlashcardDeck;
import com.ieltspath.library.domain.aggregate.Note;
import com.ieltspath.library.domain.exception.ResourceNotFoundException;
import com.ieltspath.library.domain.repository.FlashcardDeckItemRepository;
import com.ieltspath.library.domain.repository.FlashcardDeckRepository;
import com.ieltspath.library.domain.repository.FlashcardRepository;
import com.ieltspath.library.domain.repository.NoteRepository;
import com.ieltspath.library.domain.vo.FlashcardSourceType;
import com.ieltspath.library.domain.vo.LibraryStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RemoveFlashcardFromDeckUseCase {
    private final FlashcardDeckRepository decks;
    private final FlashcardRepository cards;
    private final FlashcardDeckItemRepository items;

    @Transactional
    public void execute(UUID userId, UUID deckId, UUID flashcardId) {
        ApplicationSupport.required(decks.findByIdAndUserId(deckId, userId));
        ApplicationSupport.required(cards.findByIdAndUserId(flashcardId, userId));
        items.delete(deckId, flashcardId);
    }
}
