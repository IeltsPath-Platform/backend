package com.ieltspath.community.application.usecase;

import com.ieltspath.commonstorage.UploadRejectedException;
import com.ieltspath.community.application.CommunityActor;
import com.ieltspath.community.application.port.CommunityStoragePort;
import com.ieltspath.community.application.port.MediaRateLimiter;
import com.ieltspath.community.application.result.StoredMediaResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class UploadCommunityImageUseCaseTest {

    private CommunityStoragePort storagePort;
    private MediaRateLimiter rateLimiter;
    private UploadCommunityImageUseCase useCase;

    private static final byte[] VALID_PNG = new byte[]{
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00
    };
    private static final byte[] VALID_JPEG = new byte[]{
            (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00, 0x00
    };
    private static final byte[] VALID_GIF = "GIF89a\0\0\0".getBytes();
    private static final byte[] VALID_WEBP = new byte[]{
            'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P', 0, 0
    };

    @BeforeEach
    void setUp() {
        storagePort = mock(CommunityStoragePort.class);
        rateLimiter = mock(MediaRateLimiter.class);
        useCase = new UploadCommunityImageUseCase(storagePort, rateLimiter);
    }

    @Test
    void uploadsPngSuccessfully() {
        UUID userId = UUID.randomUUID();
        CommunityActor actor = new CommunityActor(userId, false);
        when(storagePort.storeImage(anyString(), any(), eq("image/png"))).thenAnswer(inv ->
                new StoredMediaResult("https://storage/png", inv.getArgument(0), "image/png", VALID_PNG.length));

        StoredMediaResult result = useCase.execute(actor, "image/png", VALID_PNG);

        assertNotNull(result);
        assertEquals("image/png", result.contentType());
        assertTrue(result.key().startsWith("community/post/" + userId + "/"));
        assertTrue(result.key().endsWith(".png"));
        verify(rateLimiter).checkAndAcquire(userId);
        verify(storagePort).storeImage(eq(result.key()), eq(VALID_PNG), eq("image/png"));
    }

    @Test
    void uploadsJpegSuccessfully() {
        UUID userId = UUID.randomUUID();
        CommunityActor actor = new CommunityActor(userId, false);
        when(storagePort.storeImage(anyString(), any(), eq("image/jpeg"))).thenAnswer(inv ->
                new StoredMediaResult("https://storage/jpg", inv.getArgument(0), "image/jpeg", VALID_JPEG.length));

        StoredMediaResult result = useCase.execute(actor, "image/jpeg", VALID_JPEG);

        assertNotNull(result);
        assertEquals("image/jpeg", result.contentType());
        assertTrue(result.key().startsWith("community/post/" + userId + "/"));
        assertTrue(result.key().endsWith(".jpg"));
    }

    @Test
    void uploadsGifSuccessfully() {
        UUID userId = UUID.randomUUID();
        CommunityActor actor = new CommunityActor(userId, false);
        when(storagePort.storeImage(anyString(), any(), eq("image/gif"))).thenAnswer(inv ->
                new StoredMediaResult("https://storage/gif", inv.getArgument(0), "image/gif", VALID_GIF.length));

        StoredMediaResult result = useCase.execute(actor, "image/gif", VALID_GIF);

        assertNotNull(result);
        assertEquals("image/gif", result.contentType());
        assertTrue(result.key().startsWith("community/post/" + userId + "/"));
        assertTrue(result.key().endsWith(".gif"));
    }

    @Test
    void uploadsWebpSuccessfully() {
        UUID userId = UUID.randomUUID();
        CommunityActor actor = new CommunityActor(userId, false);
        when(storagePort.storeImage(anyString(), any(), eq("image/webp"))).thenAnswer(inv ->
                new StoredMediaResult("https://storage/webp", inv.getArgument(0), "image/webp", VALID_WEBP.length));

        StoredMediaResult result = useCase.execute(actor, "image/webp", VALID_WEBP);

        assertNotNull(result);
        assertEquals("image/webp", result.contentType());
        assertTrue(result.key().startsWith("community/post/" + userId + "/"));
        assertTrue(result.key().endsWith(".webp"));
    }

    @Test
    void rejectsEmptyBytes() {
        UUID userId = UUID.randomUUID();
        CommunityActor actor = new CommunityActor(userId, false);

        UploadRejectedException ex = assertThrows(UploadRejectedException.class,
                () -> useCase.execute(actor, "image/png", new byte[0]));
        assertEquals(UploadRejectedException.Reason.EMPTY, ex.reason());
        verifyNoInteractions(storagePort);
    }

    @Test
    void rejectsUnsupportedMime() {
        UUID userId = UUID.randomUUID();
        CommunityActor actor = new CommunityActor(userId, false);

        UploadRejectedException ex = assertThrows(UploadRejectedException.class,
                () -> useCase.execute(actor, "application/pdf", VALID_PNG));
        assertEquals(UploadRejectedException.Reason.TYPE_NOT_ALLOWED, ex.reason());
        verifyNoInteractions(storagePort);
    }

    @Test
    void rejectsMismatchedMagicBytes() {
        UUID userId = UUID.randomUUID();
        CommunityActor actor = new CommunityActor(userId, false);

        UploadRejectedException ex = assertThrows(UploadRejectedException.class,
                () -> useCase.execute(actor, "image/png", VALID_JPEG));
        assertEquals(UploadRejectedException.Reason.CONTENT_MISMATCH, ex.reason());
        verifyNoInteractions(storagePort);
    }

    @Test
    void rejectsTooLargeImage() {
        UUID userId = UUID.randomUUID();
        CommunityActor actor = new CommunityActor(userId, false);
        byte[] oversized = new byte[5 * 1024 * 1024 + 1];
        System.arraycopy(VALID_PNG, 0, oversized, 0, VALID_PNG.length);

        UploadRejectedException ex = assertThrows(UploadRejectedException.class,
                () -> useCase.execute(actor, "image/png", oversized));
        assertEquals(UploadRejectedException.Reason.TOO_LARGE, ex.reason());
        verifyNoInteractions(storagePort);
    }
}

