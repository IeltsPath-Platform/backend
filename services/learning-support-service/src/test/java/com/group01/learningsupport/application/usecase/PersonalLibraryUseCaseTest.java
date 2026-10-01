package com.group01.learningsupport.application.usecase;

import com.group01.learningsupport.domain.exception.InvalidDataException;
import com.group01.learningsupport.domain.aggregate.Flashcard;
import com.group01.learningsupport.domain.aggregate.FlashcardDeck;
import com.group01.learningsupport.domain.exception.ConflictException;
import com.group01.learningsupport.domain.exception.ResourceNotFoundException;
import com.group01.learningsupport.domain.repository.FlashcardDeckItemRepository;
import com.group01.learningsupport.domain.repository.FlashcardDeckRepository;
import com.group01.learningsupport.domain.repository.FlashcardRepository;
import com.group01.learningsupport.domain.vo.FlashcardSourceType;
import com.group01.learningsupport.domain.vo.LibraryStatus;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PersonalLibraryUseCaseTest {
    @Test
    void manualFlashcardRejectsSourceFields() {
        assertThrows(InvalidDataException.class, () -> Flashcard.create(
                UUID.randomUUID(),
                FlashcardSourceType.MANUAL,
                UUID.randomUUID(),
                null,
                null,
                "front",
                "back"
        ));
    }

    @Test
    void addItemRequiresActiveOwnedCard() {
        FlashcardDeckRepository decks = mock(FlashcardDeckRepository.class);
        FlashcardRepository cards = mock(FlashcardRepository.class);
        FlashcardDeckItemRepository items = mock(FlashcardDeckItemRepository.class);
        AddFlashcardToDeckUseCase useCase = new AddFlashcardToDeckUseCase(decks, cards, items);
        UUID userId = UUID.randomUUID();
        FlashcardDeck deck = FlashcardDeck.create(userId, "Deck", null);
        Flashcard card = Flashcard.create(userId, FlashcardSourceType.MANUAL, null, null, null, "front", "back");
        card.update(FlashcardSourceType.MANUAL, null, null, null, "front", "back", LibraryStatus.ARCHIVED);
        when(decks.findByIdAndUserId(deck.getId(), userId)).thenReturn(Optional.of(deck));
        when(cards.findByIdAndUserId(card.getId(), userId)).thenReturn(Optional.of(card));

        assertThrows(ResourceNotFoundException.class, () -> useCase.execute(userId, deck.getId(), card.getId(), 1));
        verify(items, never()).add(any(), any(), any());
    }

    @Test
    void addItemHidesAnotherUsersCard() {
        FlashcardDeckRepository decks = mock(FlashcardDeckRepository.class);
        FlashcardRepository cards = mock(FlashcardRepository.class);
        FlashcardDeckItemRepository items = mock(FlashcardDeckItemRepository.class);
        AddFlashcardToDeckUseCase useCase = new AddFlashcardToDeckUseCase(decks, cards, items);
        UUID userId = UUID.randomUUID();
        UUID cardId = UUID.randomUUID();
        FlashcardDeck deck = FlashcardDeck.create(userId, "Deck", null);
        when(decks.findByIdAndUserId(deck.getId(), userId)).thenReturn(Optional.of(deck));
        when(cards.findByIdAndUserId(cardId, userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> useCase.execute(userId, deck.getId(), cardId, null));
        verifyNoInteractions(items);
    }

    @Test
    void deleteDeckRemovesMembershipsAndKeepsFlashcards() {
        FlashcardDeckRepository decks = mock(FlashcardDeckRepository.class);
        FlashcardDeckItemRepository items = mock(FlashcardDeckItemRepository.class);
        FlashcardRepository cards = mock(FlashcardRepository.class);
        DeleteFlashcardDeckUseCase useCase = new DeleteFlashcardDeckUseCase(decks, items);
        UUID userId = UUID.randomUUID();
        FlashcardDeck deck = FlashcardDeck.create(userId, "Deck", null);
        when(decks.findByIdAndUserId(deck.getId(), userId)).thenReturn(Optional.of(deck));
        when(decks.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        useCase.execute(userId, deck.getId());

        assertEquals(LibraryStatus.DELETED, deck.getStatus());
        verify(items).deleteByDeckId(deck.getId());
        verifyNoInteractions(cards);
    }

    @Test
    void duplicateActiveDeckNameSurfacesConflict() {
        FlashcardDeckRepository decks = mock(FlashcardDeckRepository.class);
        CreateFlashcardDeckUseCase useCase = new CreateFlashcardDeckUseCase(decks);
        when(decks.save(any())).thenThrow(new ConflictException());

        assertThrows(ConflictException.class, () -> useCase.execute(UUID.randomUUID(), "Academic Vocabulary", null));
    }

    @Test
    void duplicateDeckMembershipSurfacesConflict() {
        FlashcardDeckRepository decks = mock(FlashcardDeckRepository.class);
        FlashcardRepository cards = mock(FlashcardRepository.class);
        FlashcardDeckItemRepository items = mock(FlashcardDeckItemRepository.class);
        AddFlashcardToDeckUseCase useCase = new AddFlashcardToDeckUseCase(decks, cards, items);
        UUID userId = UUID.randomUUID();
        FlashcardDeck deck = FlashcardDeck.create(userId, "Deck", null);
        Flashcard card = Flashcard.create(userId, FlashcardSourceType.MANUAL, null, null, null, "front", "back");
        when(decks.findByIdAndUserId(deck.getId(), userId)).thenReturn(Optional.of(deck));
        when(cards.findByIdAndUserId(card.getId(), userId)).thenReturn(Optional.of(card));
        org.mockito.Mockito.doThrow(new ConflictException()).when(items).add(deck.getId(), card.getId(), 1);

        assertThrows(ConflictException.class, () -> useCase.execute(userId, deck.getId(), card.getId(), 1));
    }
}
