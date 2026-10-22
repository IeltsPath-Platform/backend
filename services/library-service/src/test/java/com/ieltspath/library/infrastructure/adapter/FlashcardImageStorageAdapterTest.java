package com.ieltspath.library.infrastructure.adapter;

import com.ieltspath.commonstorage.ObjectStorage;
import com.ieltspath.commonstorage.StorageException;
import com.ieltspath.commonstorage.StoredObject;
import com.ieltspath.library.application.result.StoredImageResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FlashcardImageStorageAdapterTest {

    private ObjectStorage objectStorage;
    private FlashcardImageStorageAdapter adapter;
    private static final String BUCKET = "flashcards";

    @BeforeEach
    void setUp() {
        objectStorage = mock(ObjectStorage.class);
        adapter = new FlashcardImageStorageAdapter(objectStorage, BUCKET);
    }

    @Test
    void storePutsObjectAndReturnsResult() {
        byte[] content = new byte[]{1, 2, 3};
        String key = "images/user/uuid.png";
        when(objectStorage.put(BUCKET, key, content, "image/png"))
                .thenReturn(new StoredObject(BUCKET, key, "image/png", 3L));
        when(objectStorage.publicUrl(BUCKET, key)).thenReturn("https://storage/images/user/uuid.png");

        StoredImageResult result = adapter.store(key, content, "image/png");

        assertNotNull(result);
        assertEquals(key, result.key());
        assertEquals("https://storage/images/user/uuid.png", result.url());
        assertEquals("image/png", result.contentType());
        assertEquals(3, result.size());
    }

    @Test
    void storeCleansUpOnOutcomeUnknown() {
        byte[] content = new byte[]{1, 2, 3};
        String key = "images/user/uuid.png";
        StorageException ex = new StorageException("put", StorageException.Reason.UNAVAILABLE, 503, true, "abcdef12");
        when(objectStorage.put(BUCKET, key, content, "image/png")).thenThrow(ex);

        StorageException thrown = assertThrows(StorageException.class, () -> adapter.store(key, content, "image/png"));
        assertSame(ex, thrown);
        verify(objectStorage).delete(BUCKET, key);
    }

    @Test
    void publicUrlDelegatesToObjectStorage() {
        when(objectStorage.publicUrl(BUCKET, "key1")).thenReturn("https://url/key1");
        assertEquals("https://url/key1", adapter.publicUrl("key1"));
        assertNull(adapter.publicUrl(null));
        assertNull(adapter.publicUrl(""));
    }

    @Test
    void existsDelegatesToObjectStorage() {
        when(objectStorage.exists(BUCKET, "key1")).thenReturn(true);
        assertTrue(adapter.exists("key1"));
        assertFalse(adapter.exists(null));
        assertFalse(adapter.exists(""));
    }

    @Test
    void deleteDelegatesToObjectStorage() {
        adapter.delete("key1");
        verify(objectStorage).delete(BUCKET, "key1");
        adapter.delete(null);
        verifyNoMoreInteractions(objectStorage);
    }
}
