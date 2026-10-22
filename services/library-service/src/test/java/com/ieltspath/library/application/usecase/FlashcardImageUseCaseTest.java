package com.ieltspath.library.application.usecase;

import com.ieltspath.commonstorage.key.ObjectKeys;
import com.ieltspath.library.application.port.FlashcardImageStorage;
import com.ieltspath.library.application.result.FlashcardResult;
import com.ieltspath.library.application.service.FlashcardImageUsageService;
import com.ieltspath.library.domain.aggregate.Flashcard;
import com.ieltspath.library.domain.exception.InvalidDataException;
import com.ieltspath.library.domain.repository.FlashcardDeckItemRepository;
import com.ieltspath.library.domain.repository.FlashcardRepository;
import com.ieltspath.library.domain.vo.FlashcardSourceType;
import com.ieltspath.library.domain.vo.LibraryStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class FlashcardImageUseCaseTest {

    private FlashcardRepository repository;
    private FlashcardDeckItemRepository items;
    private FlashcardImageStorage storage;
    private FlashcardImageUsageService imageUsage;
    private CreateFlashcardUseCase createUseCase;
    private UpdateFlashcardUseCase updateUseCase;
    private DeleteFlashcardUseCase deleteUseCase;

    private final UUID userId = UUID.randomUUID();
    private final UUID otherUserId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        repository = mock(FlashcardRepository.class);
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

        createUseCase = new CreateFlashcardUseCase(repository, storage, imageUsage, transactionTemplate);
        updateUseCase = new UpdateFlashcardUseCase(repository, items, storage, imageUsage, transactionTemplate);
        deleteUseCase = new DeleteFlashcardUseCase(repository, items, storage, imageUsage, transactionTemplate);
    }

    @Test
    void createCardWithOtherUserKeyThrowsInvalidImageKey() {
        String otherUserKey = ObjectKeys.forUser("images", otherUserId, "png");

        InvalidDataException ex = assertThrows(InvalidDataException.class, () ->
                createUseCase.execute(userId, FlashcardSourceType.MANUAL, null, null, null,
                        "front", "back", otherUserKey, null));
        assertEquals("INVALID_IMAGE_KEY", ex.getMessage());
        verify(storage, never()).exists(any());
        verify(repository, never()).save(any());
    }

    @Test
    void createCardWithNonExistentKeyThrowsInvalidImageKey() {
        String myKey = ObjectKeys.forUser("images", userId, "png");
        when(storage.exists(myKey)).thenReturn(false);

        InvalidDataException ex = assertThrows(InvalidDataException.class, () ->
                createUseCase.execute(userId, FlashcardSourceType.MANUAL, null, null, null,
                        "front", "back", myKey, null));
        assertEquals("INVALID_IMAGE_KEY", ex.getMessage());
        verify(storage).exists(myKey);
        verify(repository, never()).save(any());
    }

    @Test
    void createCardWithKeyAlreadyInUseThrowsInvalidImageKey() {
        String myKey = ObjectKeys.forUser("images", userId, "png");
        when(storage.exists(myKey)).thenReturn(true);
        when(imageUsage.isImageKeyInUseByOther(myKey, null, null)).thenReturn(true);

        InvalidDataException ex = assertThrows(InvalidDataException.class, () ->
                createUseCase.execute(userId, FlashcardSourceType.MANUAL, null, null, null,
                        "front", "back", myKey, null));
        assertEquals("INVALID_IMAGE_KEY", ex.getMessage());
        verify(repository, never()).save(any());
    }

    @Test
    void createCardWithValidKeySavesAndComputesUrl() {
        String myKey = ObjectKeys.forUser("images", userId, "png");
        when(storage.exists(myKey)).thenReturn(true);
        when(imageUsage.isImageKeyInUseByOther(myKey, null, null)).thenReturn(false);
        when(storage.publicUrl(myKey)).thenReturn("https://storage/my-image.png");
        when(repository.save(any(Flashcard.class))).thenAnswer(inv -> inv.getArgument(0));

        FlashcardResult result = createUseCase.execute(userId, FlashcardSourceType.MANUAL, null, null, null,
                "front", "back", myKey, "My example");

        assertNotNull(result);
        assertEquals(myKey, result.imageKey());
        assertEquals("https://storage/my-image.png", result.imageUrl());
        assertEquals("My example", result.exampleSentence());
        verify(storage).exists(myKey);
        verify(repository).save(any(Flashcard.class));
    }

    @Test
    void updateCardWithUnchangedKeyDoesNotCallStorageExists() {
        UUID cardId = UUID.randomUUID();
        String currentKey = ObjectKeys.forUser("images", userId, "png");
        Flashcard existingCard = Flashcard.create(userId, FlashcardSourceType.MANUAL, null, null, null,
                "front", "back", currentKey, "example");

        when(repository.findByIdAndUserId(cardId, userId)).thenReturn(Optional.of(existingCard));
        when(repository.save(any(Flashcard.class))).thenAnswer(inv -> inv.getArgument(0));
        when(storage.publicUrl(currentKey)).thenReturn("https://storage/existing.png");

        FlashcardResult result = updateUseCase.execute(userId, cardId, FlashcardSourceType.MANUAL, null, null, null,
                "new front", "new back", LibraryStatus.ACTIVE, true, currentKey, "example");

        assertNotNull(result);
        assertEquals(currentKey, result.imageKey());
        verify(storage, never()).exists(currentKey);
        verify(storage, never()).deleteAfterCommit(any());
    }

    @Test
    void updateCardWithoutImageKeySpecifiedPreservesExistingImage() {
        UUID cardId = UUID.randomUUID();
        String currentKey = ObjectKeys.forUser("images", userId, "png");
        Flashcard existingCard = Flashcard.create(userId, FlashcardSourceType.MANUAL, null, null, null,
                "front", "back", currentKey, "example");

        when(repository.findByIdAndUserId(cardId, userId)).thenReturn(Optional.of(existingCard));
        when(repository.save(any(Flashcard.class))).thenAnswer(inv -> inv.getArgument(0));
        when(storage.publicUrl(currentKey)).thenReturn("https://storage/existing.png");

        // Client omitted imageKey (imageKeySpecified = false, imageKey = null)
        FlashcardResult result = updateUseCase.execute(userId, cardId, FlashcardSourceType.MANUAL, null, null, null,
                "updated front", "updated back", LibraryStatus.ACTIVE, false, null, "example");

        assertNotNull(result);
        assertEquals(currentKey, result.imageKey());
        verify(storage, never()).exists(any());
        verify(storage, never()).deleteAfterCommit(any());
    }

    @Test
    void updateCardWithNewKeyValidatesAndDeletesOldKeyIfNotReferenced() {
        UUID cardId = UUID.randomUUID();
        String oldKey = ObjectKeys.forUser("images", userId, "png");
        String newKey = ObjectKeys.forUser("images", userId, "png");
        Flashcard existingCard = Flashcard.create(userId, FlashcardSourceType.MANUAL, null, null, null,
                "front", "back", oldKey, "example");

        when(repository.findByIdAndUserId(cardId, userId)).thenReturn(Optional.of(existingCard));
        when(storage.exists(newKey)).thenReturn(true);
        when(imageUsage.isImageKeyInUseByOther(newKey, cardId, null)).thenReturn(false);
        when(imageUsage.isStillReferenced(oldKey)).thenReturn(false);
        when(repository.save(any(Flashcard.class))).thenAnswer(inv -> inv.getArgument(0));

        updateUseCase.execute(userId, cardId, FlashcardSourceType.MANUAL, null, null, null,
                "front", "back", LibraryStatus.ACTIVE, true, newKey, "example");

        verify(storage).exists(newKey);
        verify(storage).deleteAfterCommit(oldKey);
    }

    @Test
    void updateCardWithNewKeyDoesNotDeleteOldKeyIfStillReferencedElsewhere() {
        UUID cardId = UUID.randomUUID();
        String oldKey = ObjectKeys.forUser("images", userId, "png");
        String newKey = ObjectKeys.forUser("images", userId, "png");
        Flashcard existingCard = Flashcard.create(userId, FlashcardSourceType.MANUAL, null, null, null,
                "front", "back", oldKey, "example");

        when(repository.findByIdAndUserId(cardId, userId)).thenReturn(Optional.of(existingCard));
        when(storage.exists(newKey)).thenReturn(true);
        when(imageUsage.isImageKeyInUseByOther(newKey, cardId, null)).thenReturn(false);
        when(imageUsage.isStillReferenced(oldKey)).thenReturn(true); // still used by another card or deck
        when(repository.save(any(Flashcard.class))).thenAnswer(inv -> inv.getArgument(0));

        updateUseCase.execute(userId, cardId, FlashcardSourceType.MANUAL, null, null, null,
                "front", "back", LibraryStatus.ACTIVE, true, newKey, "example");

        verify(storage).exists(newKey);
        verify(storage, never()).deleteAfterCommit(oldKey);
    }

    @Test
    void updateCardRemovingImageDeletesOldKeyIfNotReferenced() {
        UUID cardId = UUID.randomUUID();
        String oldKey = ObjectKeys.forUser("images", userId, "png");
        Flashcard existingCard = Flashcard.create(userId, FlashcardSourceType.MANUAL, null, null, null,
                "front", "back", oldKey, "example");

        when(repository.findByIdAndUserId(cardId, userId)).thenReturn(Optional.of(existingCard));
        when(imageUsage.isStillReferenced(oldKey)).thenReturn(false);
        when(repository.save(any(Flashcard.class))).thenAnswer(inv -> inv.getArgument(0));

        updateUseCase.execute(userId, cardId, FlashcardSourceType.MANUAL, null, null, null,
                "front", "back", LibraryStatus.ACTIVE, true, null, "example");

        verify(storage, never()).exists(any());
        verify(storage).deleteAfterCommit(oldKey);
    }

    @Test
    void deleteCardDeletesOldKeyIfNotReferencedElsewhere() {
        UUID cardId = UUID.randomUUID();
        String oldKey = ObjectKeys.forUser("images", userId, "png");
        Flashcard existingCard = Flashcard.create(userId, FlashcardSourceType.MANUAL, null, null, null,
                "front", "back", oldKey, "example");

        when(repository.findByIdAndUserId(cardId, userId)).thenReturn(Optional.of(existingCard));
        when(imageUsage.isStillReferenced(oldKey)).thenReturn(false);
        when(repository.save(any(Flashcard.class))).thenAnswer(inv -> inv.getArgument(0));

        deleteUseCase.execute(userId, cardId);

        verify(repository).save(existingCard);
        verify(items).deleteByFlashcardId(cardId);
        verify(storage).deleteAfterCommit(oldKey);
    }

    @Test
    void deleteCardDoesNotDeleteOldKeyIfStillReferencedElsewhere() {
        UUID cardId = UUID.randomUUID();
        String oldKey = ObjectKeys.forUser("images", userId, "png");
        Flashcard existingCard = Flashcard.create(userId, FlashcardSourceType.MANUAL, null, null, null,
                "front", "back", oldKey, "example");

        when(repository.findByIdAndUserId(cardId, userId)).thenReturn(Optional.of(existingCard));
        when(imageUsage.isStillReferenced(oldKey)).thenReturn(true);
        when(repository.save(any(Flashcard.class))).thenAnswer(inv -> inv.getArgument(0));

        deleteUseCase.execute(userId, cardId);

        verify(repository).save(existingCard);
        verify(items).deleteByFlashcardId(cardId);
        verify(storage, never()).deleteAfterCommit(oldKey);
    }
}
