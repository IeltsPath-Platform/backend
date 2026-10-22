package com.ieltspath.library.application.usecase;

import com.ieltspath.commonstorage.key.ObjectKeys;
import com.ieltspath.library.application.port.FlashcardImageStorage;
import com.ieltspath.library.application.result.FlashcardResult;
import com.ieltspath.library.domain.aggregate.Flashcard;
import com.ieltspath.library.domain.exception.InvalidDataException;
import com.ieltspath.library.domain.repository.FlashcardDeckItemRepository;
import com.ieltspath.library.domain.repository.FlashcardRepository;
import com.ieltspath.library.domain.vo.FlashcardSourceType;
import com.ieltspath.library.domain.vo.LibraryStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
    private CreateFlashcardUseCase createUseCase;
    private UpdateFlashcardUseCase updateUseCase;

    private final UUID userId = UUID.randomUUID();
    private final UUID otherUserId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        repository = mock(FlashcardRepository.class);
        items = mock(FlashcardDeckItemRepository.class);
        storage = mock(FlashcardImageStorage.class);
        createUseCase = new CreateFlashcardUseCase(repository, storage);
        updateUseCase = new UpdateFlashcardUseCase(repository, items, storage);
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
    void createCardWithValidKeySavesAndComputesUrl() {
        String myKey = ObjectKeys.forUser("images", userId, "png");
        when(storage.exists(myKey)).thenReturn(true);
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
                "new front", "new back", LibraryStatus.ACTIVE, currentKey, "example");

        assertNotNull(result);
        assertEquals(currentKey, result.imageKey());
        verify(storage, never()).exists(currentKey);
        verify(storage, never()).delete(any());
    }

    @Test
    void updateCardWithNewKeyValidatesAndDeletesOldKey() {
        UUID cardId = UUID.randomUUID();
        String oldKey = ObjectKeys.forUser("images", userId, "png");
        String newKey = ObjectKeys.forUser("images", userId, "png");
        Flashcard existingCard = Flashcard.create(userId, FlashcardSourceType.MANUAL, null, null, null,
                "front", "back", oldKey, "example");

        when(repository.findByIdAndUserId(cardId, userId)).thenReturn(Optional.of(existingCard));
        when(storage.exists(newKey)).thenReturn(true);
        when(repository.save(any(Flashcard.class))).thenAnswer(inv -> inv.getArgument(0));

        updateUseCase.execute(userId, cardId, FlashcardSourceType.MANUAL, null, null, null,
                "front", "back", LibraryStatus.ACTIVE, newKey, "example");

        verify(storage).exists(newKey);
        verify(storage).delete(oldKey);
    }

    @Test
    void updateCardRemovingImageDeletesOldKey() {
        UUID cardId = UUID.randomUUID();
        String oldKey = ObjectKeys.forUser("images", userId, "png");
        Flashcard existingCard = Flashcard.create(userId, FlashcardSourceType.MANUAL, null, null, null,
                "front", "back", oldKey, "example");

        when(repository.findByIdAndUserId(cardId, userId)).thenReturn(Optional.of(existingCard));
        when(repository.save(any(Flashcard.class))).thenAnswer(inv -> inv.getArgument(0));

        updateUseCase.execute(userId, cardId, FlashcardSourceType.MANUAL, null, null, null,
                "front", "back", LibraryStatus.ACTIVE, null, "example");

        verify(storage, never()).exists(any());
        verify(storage).delete(oldKey);
    }
}
