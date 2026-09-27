package com.group01.learningsupport.application.usecase;

import com.group01.learningsupport.application.ApplicationSupport;
import com.group01.learningsupport.domain.aggregate.FlashcardDeck;
import com.group01.learningsupport.domain.repository.FlashcardDeckRepository;
import com.group01.learningsupport.domain.vo.LibraryStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeleteFlashcardDeckUseCase {
    private final FlashcardDeckRepository repository;

    @Transactional
    public void execute(UUID userId, UUID deckId) {
        FlashcardDeck deck = ApplicationSupport.required(repository.findByIdAndUserId(deckId, userId));
        deck.update(deck.getName(), deck.getDescription(), LibraryStatus.DELETED);
        repository.save(deck);
    }
}
