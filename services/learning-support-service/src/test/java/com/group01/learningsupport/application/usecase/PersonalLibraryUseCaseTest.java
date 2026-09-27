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
        when(cards.findAvailableByIdAndUserId(card.getId(), userId)).thenReturn(Optional.of(card));

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
        when(cards.findAvailableByIdAndUserId(cardId, userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> useCase.execute(userId, deck.getId(), cardId, null));
        verifyNoInteractions(items);
    }

    @Test
    void deleteDeckPreservesMembershipsForRestoration() {
        FlashcardDeckRepository decks = mock(FlashcardDeckRepository.class);
        FlashcardDeckItemRepository items = mock(FlashcardDeckItemRepository.class);
        FlashcardRepository cards = mock(FlashcardRepository.class);
        DeleteFlashcardDeckUseCase useCase = new DeleteFlashcardDeckUseCase(decks);
        UUID userId = UUID.randomUUID();
        FlashcardDeck deck = FlashcardDeck.create(userId, "Deck", null);
        when(decks.findByIdAndUserId(deck.getId(), userId)).thenReturn(Optional.of(deck));
        when(decks.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        useCase.execute(userId, deck.getId());

        assertEquals(LibraryStatus.DELETED, deck.getStatus());
        verifyNoInteractions(items);
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
        when(cards.findAvailableByIdAndUserId(card.getId(), userId)).thenReturn(Optional.of(card));
        org.mockito.Mockito.doThrow(new ConflictException()).when(items).add(deck.getId(), card.getId(), 1);

        assertThrows(ConflictException.class, () -> useCase.execute(userId, deck.getId(), card.getId(), 1));
    }

    @Test
    void highlightedFlashcardSupportsUserDefinedContentWithoutVocabularyLookup() {
        UUID sourceId = UUID.randomUUID();

        Flashcard card = Flashcard.create(
                UUID.randomUUID(),
                FlashcardSourceType.HIGHLIGHT,
                null,
                sourceId,
                "in spite of",
                "in spite of",
                "mac du"
        );

        assertEquals(FlashcardSourceType.HIGHLIGHT, card.getSourceType());
        assertEquals(sourceId, card.getSourceReferenceId());
        assertEquals("in spite of", card.getHighlightedText());
        assertEquals("mac du", card.getBack());
    }

    @Test
    void updateCannotBeUsedAsDeleteOperation() {
        Flashcard card = Flashcard.create(
                UUID.randomUUID(), FlashcardSourceType.MANUAL, null, null, null, "front", "back"
        );

        assertThrows(InvalidDataException.class, () -> card.update(
                FlashcardSourceType.MANUAL, null, null, null, "front", "back", LibraryStatus.DELETED
        ));
    }

    @Test
    void softDeleteHidesFlashcardAndPreservesDeckMemberships() {
        FlashcardRepository cards = mock(FlashcardRepository.class);
        FlashcardDeckItemRepository items = mock(FlashcardDeckItemRepository.class);
        DeleteFlashcardUseCase useCase = new DeleteFlashcardUseCase(cards);
        UUID userId = UUID.randomUUID();
        Flashcard card = Flashcard.create(
                userId, FlashcardSourceType.MANUAL, null, null, null, "front", "back"
        );
        when(cards.findAvailableByIdAndUserId(card.getId(), userId)).thenReturn(Optional.of(card));
        when(cards.save(card)).thenReturn(card);

        useCase.execute(userId, card.getId());

        assertEquals(LibraryStatus.DELETED, card.getStatus());
        verify(cards).save(card);
        verifyNoInteractions(items);
    }

    @Test
    void restoringDeletedFlashcardKeepsItsOldMemberships() {
        FlashcardRepository cards = mock(FlashcardRepository.class);
        FlashcardDeckItemRepository items = mock(FlashcardDeckItemRepository.class);
        UUID userId = UUID.randomUUID();
        Flashcard card = Flashcard.create(userId, FlashcardSourceType.MANUAL, null, null, null, "front", "back");
        card.delete();
        when(cards.findByIdAndUserId(card.getId(), userId)).thenReturn(Optional.of(card));
        when(cards.save(card)).thenReturn(card);

        new RestoreFlashcardUseCase(cards).execute(userId, card.getId());

        assertEquals(LibraryStatus.ACTIVE, card.getStatus());
        verifyNoInteractions(items);
    }

    @Test
    void restoringDeletedDeckKeepsItsOldMemberships() {
        FlashcardDeckRepository decks = mock(FlashcardDeckRepository.class);
        FlashcardDeckItemRepository items = mock(FlashcardDeckItemRepository.class);
        UUID userId = UUID.randomUUID();
        FlashcardDeck deck = FlashcardDeck.create(userId, "Deck", null);
        deck.update(deck.getName(), deck.getDescription(), LibraryStatus.DELETED);
        when(decks.findByIdAndUserId(deck.getId(), userId)).thenReturn(Optional.of(deck));
        when(decks.save(deck)).thenReturn(deck);

        new RestoreFlashcardDeckUseCase(decks).execute(userId, deck.getId());

        assertEquals(LibraryStatus.ACTIVE, deck.getStatus());
        verifyNoInteractions(items);
    }

    @Test
    void deletedDeckCannotListItsPreservedItems() {
        FlashcardDeckRepository decks = mock(FlashcardDeckRepository.class);
        FlashcardDeckItemRepository items = mock(FlashcardDeckItemRepository.class);
        UUID userId = UUID.randomUUID();
        FlashcardDeck deck = FlashcardDeck.create(userId, "Deck", null);
        deck.update(deck.getName(), deck.getDescription(), LibraryStatus.DELETED);
        when(decks.findByIdAndUserId(deck.getId(), userId)).thenReturn(Optional.of(deck));

        assertThrows(ResourceNotFoundException.class, () -> new ListDeckItemsUseCase(decks, items)
                .execute(userId, deck.getId(), new com.group01.learningsupport.application.query.PageQuery(0, 20)));
        verifyNoInteractions(items);
    }

    @Test
    void anotherUserCannotRestoreDeletedFlashcard() {
        FlashcardRepository cards = mock(FlashcardRepository.class);
        UUID userId = UUID.randomUUID();
        UUID cardId = UUID.randomUUID();

        assertThrows(ResourceNotFoundException.class,
                () -> new RestoreFlashcardUseCase(cards).execute(userId, cardId));
        verify(cards, never()).save(any());
    }

    @Test
    void explicitRemovalStillWorksForDeletedFlashcard() {
        FlashcardDeckRepository decks = mock(FlashcardDeckRepository.class);
        FlashcardRepository cards = mock(FlashcardRepository.class);
        FlashcardDeckItemRepository items = mock(FlashcardDeckItemRepository.class);
        UUID userId = UUID.randomUUID();
        FlashcardDeck deck = FlashcardDeck.create(userId, "Deck", null);
        Flashcard card = Flashcard.create(userId, FlashcardSourceType.MANUAL, null, null, null, "front", "back");
        card.delete();
        when(decks.findByIdAndUserId(deck.getId(), userId)).thenReturn(Optional.of(deck));
        when(cards.findByIdAndUserId(card.getId(), userId)).thenReturn(Optional.of(card));

        new RemoveFlashcardFromDeckUseCase(decks, cards, items).execute(userId, deck.getId(), card.getId());

        verify(items).delete(deck.getId(), card.getId());
    }

    @Test
    void deletedFlashcardsCannotBeListed() {
        FlashcardRepository cards = mock(FlashcardRepository.class);
        ListFlashcardsUseCase useCase = new ListFlashcardsUseCase(cards);

        assertThrows(InvalidDataException.class, () -> useCase.execute(
                UUID.randomUUID(), LibraryStatus.DELETED,
                new com.group01.learningsupport.application.query.PageQuery(0, 20)
        ));
        verifyNoInteractions(cards);
    }
}
