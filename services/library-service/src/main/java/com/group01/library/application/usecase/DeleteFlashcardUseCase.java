package com.group01.library.application.usecase;

import com.group01.library.application.ApplicationSupport;
import com.group01.library.application.query.PageQuery;
import com.group01.library.application.result.PageResult;
import com.group01.library.domain.aggregate.Flashcard;
import com.group01.library.domain.aggregate.FlashcardDeck;
import com.group01.library.domain.aggregate.Note;
import com.group01.library.domain.exception.ResourceNotFoundException;
import com.group01.library.domain.repository.FlashcardDeckItemRepository;
import com.group01.library.domain.repository.FlashcardDeckRepository;
import com.group01.library.domain.repository.FlashcardRepository;
import com.group01.library.domain.repository.NoteRepository;
import com.group01.library.domain.vo.FlashcardSourceType;
import com.group01.library.domain.vo.LibraryStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeleteFlashcardUseCase {
    private final FlashcardRepository repository;
    private final FlashcardDeckItemRepository items;

    @Transactional
    public void execute(UUID userId, UUID flashcardId) {
        Flashcard card = ApplicationSupport.required(repository.findByIdAndUserId(flashcardId, userId));
        card.update(
                card.getSourceType(),
                card.getVocabularySenseId(),
                card.getSourceReferenceId(),
                card.getHighlightedText(),
                card.getFront(),
                card.getBack(),
                LibraryStatus.DELETED
        );
        repository.save(card);
        items.deleteByFlashcardId(flashcardId);
    }
}
