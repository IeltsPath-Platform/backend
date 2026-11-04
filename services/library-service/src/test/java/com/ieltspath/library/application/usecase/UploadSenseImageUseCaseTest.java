package com.ieltspath.library.application.usecase;

import com.ieltspath.commonstorage.UploadRejectedException;
import com.ieltspath.library.application.port.CatalogMediaStorage;
import com.ieltspath.library.application.result.StoredImageResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class UploadSenseImageUseCaseTest {
    private static final byte[] PNG = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0, 0};
    private static final byte[] WEBP = {'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P', 0, 0};

    private CatalogMediaStorage storage;
    private UploadSenseImageUseCase useCase;

    @BeforeEach
    void setUp() {
        storage = mock(CatalogMediaStorage.class);
        useCase = new UploadSenseImageUseCase(storage);
        when(storage.store(anyString(), any(), anyString())).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            byte[] bytes = invocation.getArgument(1);
            return new StoredImageResult(key, "https://storage.example/" + key, invocation.getArgument(2), bytes.length);
        });
    }

    @Test
    void storesTheImageUnderTheVocabularyImagesPrefixWithItsExtension() {
        StoredImageResult png = useCase.execute("image/png", PNG);
        StoredImageResult webp = useCase.execute("image/webp", WEBP);

        assertTrue(png.key().startsWith("vocabulary/images/") && png.key().endsWith(".png"));
        assertTrue(webp.key().startsWith("vocabulary/images/") && webp.key().endsWith(".webp"));
        verify(storage).store(eq(png.key()), eq(PNG), eq("image/png"));
    }

    @Test
    void refusesBytesThatAreNotTheDeclaredImage() {
        assertThrows(UploadRejectedException.class, () -> useCase.execute("image/png", WEBP));
        verifyNoInteractions(storage);
    }
}
