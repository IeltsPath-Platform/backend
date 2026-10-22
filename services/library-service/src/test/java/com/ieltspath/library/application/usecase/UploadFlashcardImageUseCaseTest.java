package com.ieltspath.library.application.usecase;

import com.ieltspath.commonstorage.UploadRejectedException;
import com.ieltspath.library.application.port.FlashcardImageStorage;
import com.ieltspath.library.application.result.StoredImageResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class UploadFlashcardImageUseCaseTest {

    private FlashcardImageStorage storage;
    private UploadFlashcardImageUseCase useCase;

    private static final byte[] VALID_PNG = new byte[]{
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00
    };
    private static final byte[] VALID_JPEG = new byte[]{
            (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00, 0x00
    };
    private static final byte[] VALID_WEBP = new byte[]{
            'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P', 0, 0
    };

    @BeforeEach
    void setUp() {
        storage = mock(FlashcardImageStorage.class);
        useCase = new UploadFlashcardImageUseCase(storage);
    }

    @Test
    void uploadsPngSuccessfully() {
        UUID userId = UUID.randomUUID();
        when(storage.store(anyString(), any(), eq("image/png"))).thenAnswer(inv ->
                new StoredImageResult(inv.getArgument(0), "https://storage/png", "image/png", VALID_PNG.length));

        StoredImageResult result = useCase.execute(userId, "image/png", VALID_PNG);

        assertNotNull(result);
        assertEquals("image/png", result.contentType());
        assertTrue(result.key().startsWith("images/" + userId + "/"));
        assertTrue(result.key().endsWith(".png"));
        verify(storage).store(eq(result.key()), eq(VALID_PNG), eq("image/png"));
    }

    @Test
    void uploadsJpegSuccessfully() {
        UUID userId = UUID.randomUUID();
        when(storage.store(anyString(), any(), eq("image/jpeg"))).thenAnswer(inv ->
                new StoredImageResult(inv.getArgument(0), "https://storage/jpeg", "image/jpeg", VALID_JPEG.length));

        StoredImageResult result = useCase.execute(userId, "image/jpeg", VALID_JPEG);

        assertNotNull(result);
        assertEquals("image/jpeg", result.contentType());
        assertTrue(result.key().startsWith("images/" + userId + "/"));
        assertTrue(result.key().endsWith(".jpg"));
        verify(storage).store(eq(result.key()), eq(VALID_JPEG), eq("image/jpeg"));
    }

    @Test
    void uploadsWebpSuccessfully() {
        UUID userId = UUID.randomUUID();
        when(storage.store(anyString(), any(), eq("image/webp"))).thenAnswer(inv ->
                new StoredImageResult(inv.getArgument(0), "https://storage/webp", "image/webp", VALID_WEBP.length));

        StoredImageResult result = useCase.execute(userId, "image/webp", VALID_WEBP);

        assertNotNull(result);
        assertEquals("image/webp", result.contentType());
        assertTrue(result.key().startsWith("images/" + userId + "/"));
        assertTrue(result.key().endsWith(".webp"));
        verify(storage).store(eq(result.key()), eq(VALID_WEBP), eq("image/webp"));
    }

    @Test
    void rejectsEmptyBytes() {
        UUID userId = UUID.randomUUID();
        UploadRejectedException ex = assertThrows(UploadRejectedException.class,
                () -> useCase.execute(userId, "image/png", new byte[0]));
        assertEquals(UploadRejectedException.Reason.EMPTY, ex.reason());
        verifyNoInteractions(storage);
    }

    @Test
    void rejectsUnsupportedMime() {
        UUID userId = UUID.randomUUID();
        UploadRejectedException ex = assertThrows(UploadRejectedException.class,
                () -> useCase.execute(userId, "application/pdf", VALID_PNG));
        assertEquals(UploadRejectedException.Reason.TYPE_NOT_ALLOWED, ex.reason());
        verifyNoInteractions(storage);
    }

    @Test
    void rejectsMismatchedMagicBytes() {
        UUID userId = UUID.randomUUID();
        UploadRejectedException ex = assertThrows(UploadRejectedException.class,
                () -> useCase.execute(userId, "image/png", VALID_JPEG));
        assertEquals(UploadRejectedException.Reason.CONTENT_MISMATCH, ex.reason());
        verifyNoInteractions(storage);
    }

    @Test
    void rejectsTooLargeImage() {
        UUID userId = UUID.randomUUID();
        byte[] oversized = new byte[2 * 1024 * 1024 + 1];
        System.arraycopy(VALID_PNG, 0, oversized, 0, VALID_PNG.length);

        UploadRejectedException ex = assertThrows(UploadRejectedException.class,
                () -> useCase.execute(userId, "image/png", oversized));
        assertEquals(UploadRejectedException.Reason.TOO_LARGE, ex.reason());
        verifyNoInteractions(storage);
    }
}
