package com.ieltspath.library.application.usecase;

import com.ieltspath.commonstorage.key.ObjectKeys;
import com.ieltspath.library.application.port.FlashcardImageStorage;
import com.ieltspath.library.application.result.FlashcardDeckResult;
import com.ieltspath.library.domain.aggregate.FlashcardDeck;
import com.ieltspath.library.domain.exception.InvalidDataException;
import com.ieltspath.library.domain.repository.FlashcardDeckItemRepository;
import com.ieltspath.library.domain.repository.FlashcardDeckRepository;
import com.ieltspath.library.domain.vo.DeckCardCounts;
import com.ieltspath.library.domain.vo.LibraryStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class FlashcardDeckImageUseCaseTest {

    private FlashcardDeckRepository repository;
    private FlashcardDeckItemRepository items;
    private FlashcardImageStorage storage;
    private CreateFlashcardDeckUseCase createUseCase;
    private UpdateFlashcardDeckUseCase updateUseCase;
    private DeleteFlashcardDeckUseCase deleteUseCase;

    private final UUID userId = UUID.randomUUID();
    private final UUID otherUserId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        repository = mock(FlashcardDeckRepository.class);
        items = mock(FlashcardDeckItemRepository.class);
        storage = mock(FlashcardImageStorage.class);
        createUseCase = new CreateFlashcardDeckUseCase(repository, storage);
        updateUseCase = new UpdateFlashcardDeckUseCase(repository, items, storage);
        deleteUseCase = new DeleteFlashcardDeckUseCase(repository, items, storage);
    }

    @Test
    void createDeckWithOtherUserCoverKeyThrowsInvalidImageKey() {
        String otherUserKey = ObjectKeys.forUser("images", otherUserId, "webp");

        InvalidDataException ex = assertThrows(InvalidDataException.class, () ->
                createUseCase.execute(userId, "IELTS Deck", "Description", otherUserKey));
        assertEquals("INVALID_IMAGE_KEY", ex.getMessage());
        verify(storage, never()).exists(any());
        verify(repository, never()).save(any());
    }

    @Test
    void createDeckWithNonExistentCoverKeyThrowsInvalidImageKey() {
        String myKey = ObjectKeys.forUser("images", userId, "webp");
        when(storage.exists(myKey)).thenReturn(false);

        InvalidDataException ex = assertThrows(InvalidDataException.class, () ->
                createUseCase.execute(userId, "IELTS Deck", "Description", myKey));
        assertEquals("INVALID_IMAGE_KEY", ex.getMessage());
        verify(storage).exists(myKey);
        verify(repository, never()).save(any());
    }

    @Test
    void createDeckWithValidCoverKeySavesAndReturnsResult() {
        String myKey = ObjectKeys.forUser("images", userId, "webp");
        when(storage.exists(myKey)).thenReturn(true);
        when(storage.publicUrl(myKey)).thenReturn("https://storage/cover.webp");
        when(repository.save(any(FlashcardDeck.class))).thenAnswer(inv -> inv.getArgument(0));

        FlashcardDeckResult result = createUseCase.execute(userId, "IELTS Deck", "Description", myKey);

        assertNotNull(result);
        assertEquals(myKey, result.coverImageKey());
        assertEquals("https://storage/cover.webp", result.coverImageUrl());
        assertEquals(0, result.cardCount());
        assertEquals(0, result.dueCount());
        verify(storage).exists(myKey);
    }

    @Test
    void updateDeckWithNewCoverKeyValidatesAndDeletesOldKey() {
        UUID deckId = UUID.randomUUID();
        String oldKey = ObjectKeys.forUser("images", userId, "webp");
        String newKey = ObjectKeys.forUser("images", userId, "webp");
        FlashcardDeck existingDeck = FlashcardDeck.create(userId, "Old Name", "Old Desc", oldKey);

        when(repository.findByIdAndUserId(deckId, userId)).thenReturn(Optional.of(existingDeck));
        when(storage.exists(newKey)).thenReturn(true);
        when(repository.save(any(FlashcardDeck.class))).thenAnswer(inv -> inv.getArgument(0));
        when(items.countCardsAndDueByDeckIds(any(), eq(userId), any())).thenReturn(Map.of(deckId, new DeckCardCounts(10, 3)));

        FlashcardDeckResult result = updateUseCase.execute(userId, deckId, "New Name", "New Desc", LibraryStatus.ACTIVE, newKey);

        assertNotNull(result);
        assertEquals(10, result.cardCount());
        assertEquals(3, result.dueCount());
        verify(storage).exists(newKey);
        verify(storage).delete(oldKey);
    }

    @Test
    void deleteDeckDeletesCoverImageKey() {
        UUID deckId = UUID.randomUUID();
        String oldKey = ObjectKeys.forUser("images", userId, "webp");
        FlashcardDeck existingDeck = FlashcardDeck.create(userId, "Old Name", "Old Desc", oldKey);

        when(repository.findByIdAndUserId(deckId, userId)).thenReturn(Optional.of(existingDeck));
        when(repository.save(any(FlashcardDeck.class))).thenAnswer(inv -> inv.getArgument(0));

        deleteUseCase.execute(userId, deckId);

        assertEquals(LibraryStatus.DELETED, existingDeck.getStatus());
        verify(items).deleteByDeckId(deckId);
        verify(storage).delete(oldKey);
    }
}
