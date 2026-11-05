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

class UploadCatalogCoverUseCaseTest {
    private FlashcardImageStorage storage;
    private UploadCatalogCoverUseCase useCase;

    private static final byte[] PNG = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0, 0};
    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0, 0};
    private static final byte[] WEBP = {'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P', 0, 0};

    @BeforeEach
    void setUp() {
        storage = mock(FlashcardImageStorage.class);
        useCase = new UploadCatalogCoverUseCase(storage);
        when(storage.store(anyString(), any(), anyString())).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            String type = invocation.getArgument(2);
            byte[] bytes = invocation.getArgument(1);
            return new StoredImageResult(key, "https://storage.example/" + key, type, bytes.length);
        });
    }

    @Test
    void acceptsSupportedImageTypesAndStoresUnderTheCatalogCoverPrefix() {
        UUID userId = UUID.randomUUID();
        byte[][] images = {PNG, JPEG, WEBP};
        String[] types = {"image/png", "image/jpeg", "image/webp"};
        String[] extensions = {".png", ".jpg", ".webp"};

        for (int i = 0; i < images.length; i++) {
            StoredImageResult result = useCase.execute(userId, types[i], images[i]);
            assertTrue(result.key().startsWith("catalog-cover/" + userId + "/"));
            assertTrue(result.key().endsWith(extensions[i]));
            assertEquals(types[i], result.contentType());
            verify(storage).store(eq(result.key()), eq(images[i]), eq(types[i]));
        }
    }

    @Test
    void rejectsSvgAndSvgBytesDeclaredAsPng() {
        assertEquals(UploadRejectedException.Reason.TYPE_NOT_ALLOWED,
                assertThrows(UploadRejectedException.class,
                        () -> useCase.execute(UUID.randomUUID(), "image/svg+xml", "<svg/>".getBytes())).reason());
        assertEquals(UploadRejectedException.Reason.CONTENT_MISMATCH,
                assertThrows(UploadRejectedException.class,
                        () -> useCase.execute(UUID.randomUUID(), "image/png", "<svg/>".getBytes())).reason());
        verifyNoInteractions(storage);
    }

    @Test
    void rejectsEmptyOversizedAndMismatchedImageBytes() {
        UUID userId = UUID.randomUUID();
        assertEquals(UploadRejectedException.Reason.EMPTY,
                assertThrows(UploadRejectedException.class, () -> useCase.execute(userId, "image/png", new byte[0])).reason());

        byte[] oversized = new byte[2 * 1024 * 1024 + 1];
        System.arraycopy(PNG, 0, oversized, 0, PNG.length);
        assertEquals(UploadRejectedException.Reason.TOO_LARGE,
                assertThrows(UploadRejectedException.class, () -> useCase.execute(userId, "image/png", oversized)).reason());

        assertEquals(UploadRejectedException.Reason.CONTENT_MISMATCH,
                assertThrows(UploadRejectedException.class, () -> useCase.execute(userId, "image/png", JPEG)).reason());
        verifyNoInteractions(storage);
    }
}
