package com.ieltspath.assessment.application.usecase;

import com.ieltspath.assessment.config.SpeakingRecordingProperties;
import com.ieltspath.commonstorage.ObjectStorage;
import com.ieltspath.commonstorage.StoredObject;
import com.ieltspath.commonstorage.UploadRejectedException;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class UploadSpeakingRecordingUseCaseTest {
    private final ObjectStorage storage = mock(ObjectStorage.class);
    private final UUID userId = UUID.randomUUID();
    private final UploadSpeakingRecordingUseCase useCase = new UploadSpeakingRecordingUseCase(
            storage, new SpeakingRecordingProperties("speaking-recordings", 90, 900));

    @Test
    void rejectsEmptyAndOversizedFilesBeforeWriting() {
        assertThrows(UploadRejectedException.class, () -> useCase.execute(userId, "audio/webm", new byte[0]));
        assertThrows(UploadRejectedException.class, () -> useCase.execute(userId, "audio/webm", new byte[5 * 1024 * 1024 + 1]));
        verifyNoInteractions(storage);
    }

    @Test
    void rejectsNonAudioBytesEvenWhenTheMimeTypeIsAllowed() {
        assertThrows(UploadRejectedException.class,
                () -> useCase.execute(userId, "audio/webm", "not an audio file".getBytes(StandardCharsets.UTF_8)));
        verifyNoInteractions(storage);
    }

    @Test
    void writesSupportedWebmAndWavUnderTheCallersSpeakingPrefix() {
        var webm = new byte[]{0x1a, 0x45, (byte) 0xdf, (byte) 0xa3, 'w', 'e', 'b', 'm'};
        var wav = "RIFF0000WAVE".getBytes(StandardCharsets.US_ASCII);
        when(storage.put(eq("speaking-recordings"), anyString(), any(), anyString()))
                .thenAnswer(call -> new StoredObject(call.getArgument(0), call.getArgument(1), call.getArgument(3), ((byte[]) call.getArgument(2)).length));

        var webmResult = useCase.execute(userId, "audio/webm;codecs=opus", webm);
        var wavResult = useCase.execute(userId, "audio/wav", wav);

        assertTrue(webmResult.mediaReference().matches("speaking/" + userId + "/[0-9a-f-]+\\.webm"));
        assertTrue(wavResult.mediaReference().matches("speaking/" + userId + "/[0-9a-f-]+\\.wav"));
        assertEquals("audio/webm", webmResult.contentType());
        assertEquals(webm.length, webmResult.sizeBytes());
        assertEquals("audio/wav", wavResult.contentType());
    }
}
