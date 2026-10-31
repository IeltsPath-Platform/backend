package com.ieltspath.content.application.usecase;

import com.ieltspath.commonstorage.UploadRejectedException;
import com.ieltspath.content.application.port.ContentMediaStorage;
import com.ieltspath.content.application.result.StoredMediaResult;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class UploadContentImageUseCaseTest {
    private final AtomicReference<String> storedKey = new AtomicReference<>();
    private final AtomicReference<String> storedType = new AtomicReference<>();
    private final ContentMediaStorage storage = (key, content, contentType) -> {
        storedKey.set(key);
        storedType.set(contentType);
        return new StoredMediaResult(key, "https://media.example.test/" + key, contentType, content.length);
    };
    private final UUID userId = UUID.randomUUID();

    @Test
    void acceptsJpegPngAndWebpAndStoresThemUnderTheAuthorsImageFolder() {
        var useCase = new UploadContentImageUseCase(storage);
        byte[][] contents = {jpeg(), png(), webp()};
        String[] types = {"image/jpeg", "image/png", "image/webp"};
        String[] extensions = {".jpg", ".png", ".webp"};

        for (int i = 0; i < contents.length; i++) {
            var result = useCase.execute(userId, types[i], contents[i]);
            assertTrue(result.key().startsWith("content-image/" + userId + "/"));
            assertTrue(result.key().endsWith(extensions[i]));
            assertEquals(types[i], storedType.get());
            assertEquals(result.key(), storedKey.get());
        }
    }

    @Test
    void rejectsSvgByDeclaredTypeAndByBytesDisguisedAsPng() {
        var useCase = new UploadContentImageUseCase(storage);
        var declaredSvg = assertThrows(UploadRejectedException.class,
                () -> useCase.execute(userId, "image/svg+xml", "<svg/>".getBytes()));
        var disguisedSvg = assertThrows(UploadRejectedException.class,
                () -> useCase.execute(userId, "image/png", "<svg/>".getBytes()));

        assertEquals(UploadRejectedException.Reason.TYPE_NOT_ALLOWED, declaredSvg.reason());
        assertEquals(UploadRejectedException.Reason.CONTENT_MISMATCH, disguisedSvg.reason());
        assertNull(storedKey.get());
    }

    @Test
    void refusesEmptyOversizeAndBytesThatDoNotMatchTheDeclaredImageType() {
        var useCase = new UploadContentImageUseCase(storage);
        assertEquals(UploadRejectedException.Reason.EMPTY,
                assertThrows(UploadRejectedException.class, () -> useCase.execute(userId, "image/png", new byte[0])).reason());

        byte[] oversize = new byte[2 * 1024 * 1024 + 1];
        System.arraycopy(png(), 0, oversize, 0, png().length);
        assertEquals(UploadRejectedException.Reason.TOO_LARGE,
                assertThrows(UploadRejectedException.class, () -> useCase.execute(userId, "image/png", oversize)).reason());

        assertEquals(UploadRejectedException.Reason.CONTENT_MISMATCH,
                assertThrows(UploadRejectedException.class,
                        () -> useCase.execute(userId, "image/png", new byte[]{1, 2, 3})).reason());
        assertNull(storedKey.get());
    }

    private static byte[] png() {
        return new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0};
    }

    private static byte[] jpeg() {
        return new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0, 0};
    }

    private static byte[] webp() {
        return new byte[]{'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P'};
    }
}
