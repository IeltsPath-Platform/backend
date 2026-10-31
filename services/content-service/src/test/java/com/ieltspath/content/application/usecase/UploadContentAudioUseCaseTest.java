package com.ieltspath.content.application.usecase;

import com.ieltspath.commonstorage.UploadRejectedException;
import com.ieltspath.content.application.port.ContentMediaStorage;
import com.ieltspath.content.application.result.StoredMediaResult;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UploadContentAudioUseCaseTest {

    private final AtomicReference<String> storedKey = new AtomicReference<>();
    private final AtomicReference<String> storedType = new AtomicReference<>();
    private final ContentMediaStorage storage = (key, content, contentType) -> {
        storedKey.set(key);
        storedType.set(contentType);
        return new StoredMediaResult(key, "https://media.example.test/" + key, contentType, content.length);
    };
    private final UploadContentAudioUseCase useCase = new UploadContentAudioUseCase(storage);
    private final UUID userId = UUID.randomUUID();

    private static byte[] mp3() {
        byte[] bytes = new byte[64];
        System.arraycopy("ID3".getBytes(StandardCharsets.US_ASCII), 0, bytes, 0, 3);
        return bytes;
    }

    @Test
    void anMp3IsStoredUnderTheUsersFolder() {
        StoredMediaResult result = useCase.execute(userId, "audio/mpeg", mp3());

        assertTrue(result.key().startsWith("content-audio/" + userId + "/"));
        assertTrue(result.key().endsWith(".mp3"));
        assertEquals("audio/mpeg", storedType.get());
        assertEquals(storedKey.get(), result.key());
    }

    @Test
    void theClientsContentTypeIsNotTrusted() {
        byte[] png = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0};

        UploadRejectedException refused = assertThrows(UploadRejectedException.class,
                () -> useCase.execute(userId, "audio/mpeg", png));

        assertEquals(UploadRejectedException.Reason.CONTENT_MISMATCH, refused.reason());
        assertNull(storedKey.get());
    }

    @Test
    void anImageTypeIsNotAllowed() {
        UploadRejectedException refused = assertThrows(UploadRejectedException.class,
                () -> useCase.execute(userId, "image/png", mp3()));

        assertEquals(UploadRejectedException.Reason.TYPE_NOT_ALLOWED, refused.reason());
    }

    @Test
    void anEmptyFileIsRefused() {
        UploadRejectedException refused = assertThrows(UploadRejectedException.class,
                () -> useCase.execute(userId, "audio/mpeg", new byte[0]));

        assertEquals(UploadRejectedException.Reason.EMPTY, refused.reason());
    }

    @Test
    void aFileOverTwentyFiveMegabytesIsRefused() {
        byte[] big = new byte[25 * 1024 * 1024 + 1];
        System.arraycopy("ID3".getBytes(StandardCharsets.US_ASCII), 0, big, 0, 3);

        UploadRejectedException refused = assertThrows(UploadRejectedException.class,
                () -> useCase.execute(userId, "audio/mpeg", big));

        assertEquals(UploadRejectedException.Reason.TOO_LARGE, refused.reason());
        assertNull(storedKey.get());
    }

    @Test
    void wavAndOggAreAccepted() {
        byte[] wav = new byte[16];
        System.arraycopy("RIFF".getBytes(StandardCharsets.US_ASCII), 0, wav, 0, 4);
        System.arraycopy("WAVE".getBytes(StandardCharsets.US_ASCII), 0, wav, 8, 4);
        byte[] ogg = new byte[16];
        System.arraycopy("OggS".getBytes(StandardCharsets.US_ASCII), 0, ogg, 0, 4);

        assertTrue(useCase.execute(userId, "audio/wav", wav).key().endsWith(".wav"));
        assertTrue(useCase.execute(userId, "audio/ogg", ogg).key().endsWith(".ogg"));
    }
}
