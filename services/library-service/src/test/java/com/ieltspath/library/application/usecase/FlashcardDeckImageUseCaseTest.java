package com.ieltspath.library.application.usecase;

import com.ieltspath.commonstorage.key.ObjectKeys;
import com.ieltspath.library.application.port.FlashcardImageStorage;
import com.ieltspath.library.application.result.FlashcardDeckResult;
import com.ieltspath.library.application.service.FlashcardImageUsageService;
import com.ieltspath.library.domain.aggregate.FlashcardDeck;
import com.ieltspath.library.domain.exception.InvalidDataException;
import com.ieltspath.library.domain.repository.FlashcardDeckItemRepository;
import com.ieltspath.library.domain.repository.FlashcardDeckRepository;
import com.ieltspath.library.domain.vo.DeckCardCounts;
import com.ieltspath.library.domain.vo.LibraryStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class FlashcardDeckImageUseCaseTest {

    private FlashcardDeckRepository repository;
    private FlashcardDeckItemRepository items;
    private FlashcardImageStorage storage;
    private FlashcardImageUsageService imageUsage;
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
        imageUsage = mock(FlashcardImageUsageService.class);

        PlatformTransactionManager tm = new PlatformTransactionManager() {
            @Override
            public TransactionStatus getTransaction(TransactionDefinition definition) throws TransactionException {
                return new SimpleTransactionStatus();
            }

            @Override
            public void commit(TransactionStatus status) throws TransactionException {
            }

            @Override
            public void rollback(TransactionStatus status) throws TransactionException {
            }
        };
        TransactionTemplate transactionTemplate = new TransactionTemplate(tm);

        createUseCase = new CreateFlashcardDeckUseCase(repository, storage, imageUsage, transactionTemplate);
        updateUseCase = new UpdateFlashcardDeckUseCase(repository, items, storage, imageUsage, transactionTemplate);
        deleteUseCase = new DeleteFlashcardDeckUseCase(repository, items, storage, imageUsage, transactionTemplate);
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
    void createDeckWithKeyAlreadyInUseThrowsInvalidImageKey() {
        String myKey = ObjectKeys.forUser("images", userId, "webp");
        when(storage.exists(myKey)).thenReturn(true);
        when(imageUsage.isImageKeyInUseByOther(myKey, null, null)).thenReturn(true);

        InvalidDataException ex = assertThrows(InvalidDataException.class, () ->
                createUseCase.execute(userId, "IELTS Deck", "Description", myKey));
        assertEquals("INVALID_IMAGE_KEY", ex.getMessage());
        verify(repository, never()).save(any());
    }

    @Test
    void createDeckWithValidCoverKeySavesAndReturnsResult() {
        String myKey = ObjectKeys.forUser("images", userId, "webp");
        when(storage.exists(myKey)).thenReturn(true);
        when(imageUsage.isImageKeyInUseByOther(myKey, null, null)).thenReturn(false);
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
    void updateDeckWithoutCoverImageKeySpecifiedPreservesExistingCover() {
        UUID deckId = UUID.randomUUID();
        String oldKey = ObjectKeys.forUser("images", userId, "webp");
        FlashcardDeck existingDeck = FlashcardDeck.create(userId, "Old Name", "Old Desc", oldKey);

        when(repository.findByIdAndUserId(deckId, userId)).thenReturn(Optional.of(existingDeck));
        when(repository.save(any(FlashcardDeck.class))).thenAnswer(inv -> inv.getArgument(0));
        when(items.countCardsAndDueByDeckIds(any(), eq(userId), any())).thenReturn(Map.of(deckId, new DeckCardCounts(5, 1)));

        FlashcardDeckResult result = updateUseCase.execute(userId, deckId, "New Name", "New Desc", LibraryStatus.ACTIVE, false, null);

        assertNotNull(result);
        assertEquals(oldKey, result.coverImageKey());
        verify(storage, never()).exists(any());
        verify(storage, never()).deleteAfterCommit(any());
    }

    @Test
    void updateDeckWithNewCoverKeyValidatesAndDeletesOldKeyIfNotReferenced() {
        UUID deckId = UUID.randomUUID();
        String oldKey = ObjectKeys.forUser("images", userId, "webp");
        String newKey = ObjectKeys.forUser("images", userId, "webp");
        FlashcardDeck existingDeck = FlashcardDeck.create(userId, "Old Name", "Old Desc", oldKey);

        when(repository.findByIdAndUserId(deckId, userId)).thenReturn(Optional.of(existingDeck));
        when(storage.exists(newKey)).thenReturn(true);
        when(imageUsage.isImageKeyInUseByOther(newKey, null, deckId)).thenReturn(false);
        when(imageUsage.isStillReferenced(oldKey)).thenReturn(false);
        when(repository.save(any(FlashcardDeck.class))).thenAnswer(inv -> inv.getArgument(0));
        when(items.countCardsAndDueByDeckIds(any(), eq(userId), any())).thenReturn(Map.of(deckId, new DeckCardCounts(10, 3)));

        FlashcardDeckResult result = updateUseCase.execute(userId, deckId, "New Name", "New Desc", LibraryStatus.ACTIVE, true, newKey);

        assertNotNull(result);
        assertEquals(10, result.cardCount());
        assertEquals(3, result.dueCount());
        verify(storage).exists(newKey);
        verify(storage).deleteAfterCommit(oldKey);
    }

    @Test
    void updateDeckWithNewCoverKeyDoesNotDeleteOldKeyIfStillReferencedElsewhere() {
        UUID deckId = UUID.randomUUID();
        String oldKey = ObjectKeys.forUser("images", userId, "webp");
        String newKey = ObjectKeys.forUser("images", userId, "webp");
        FlashcardDeck existingDeck = FlashcardDeck.create(userId, "Old Name", "Old Desc", oldKey);

        when(repository.findByIdAndUserId(deckId, userId)).thenReturn(Optional.of(existingDeck));
        when(storage.exists(newKey)).thenReturn(true);
        when(imageUsage.isImageKeyInUseByOther(newKey, null, deckId)).thenReturn(false);
        when(imageUsage.isStillReferenced(oldKey)).thenReturn(true);
        when(repository.save(any(FlashcardDeck.class))).thenAnswer(inv -> inv.getArgument(0));
        when(items.countCardsAndDueByDeckIds(any(), eq(userId), any())).thenReturn(Map.of(deckId, new DeckCardCounts(10, 3)));

        updateUseCase.execute(userId, deckId, "New Name", "New Desc", LibraryStatus.ACTIVE, true, newKey);

        verify(storage).exists(newKey);
        verify(storage, never()).deleteAfterCommit(oldKey);
    }

    @Test
    void deleteDeckDeletesCoverImageKeyIfNotReferencedElsewhere() {
        UUID deckId = UUID.randomUUID();
        String oldKey = ObjectKeys.forUser("images", userId, "webp");
        FlashcardDeck existingDeck = FlashcardDeck.create(userId, "Old Name", "Old Desc", oldKey);

        when(repository.findByIdAndUserId(deckId, userId)).thenReturn(Optional.of(existingDeck));
        when(imageUsage.isStillReferenced(oldKey)).thenReturn(false);
        when(repository.save(any(FlashcardDeck.class))).thenAnswer(inv -> inv.getArgument(0));

        deleteUseCase.execute(userId, deckId);

        assertEquals(LibraryStatus.DELETED, existingDeck.getStatus());
        verify(items).deleteByDeckId(deckId);
        verify(storage).deleteAfterCommit(oldKey);
    }

    @Test
    void deleteDeckDoesNotDeleteCoverImageKeyIfStillReferencedElsewhere() {
        UUID deckId = UUID.randomUUID();
        String oldKey = ObjectKeys.forUser("images", userId, "webp");
        FlashcardDeck existingDeck = FlashcardDeck.create(userId, "Old Name", "Old Desc", oldKey);

        when(repository.findByIdAndUserId(deckId, userId)).thenReturn(Optional.of(existingDeck));
        when(imageUsage.isStillReferenced(oldKey)).thenReturn(true);
        when(repository.save(any(FlashcardDeck.class))).thenAnswer(inv -> inv.getArgument(0));

        deleteUseCase.execute(userId, deckId);

        assertEquals(LibraryStatus.DELETED, existingDeck.getStatus());
        verify(items).deleteByDeckId(deckId);
        verify(storage, never()).deleteAfterCommit(oldKey);
    }
}
