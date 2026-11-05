package com.ieltspath.assessment.application.usecase;

import com.ieltspath.assessment.application.port.SpeakingRecordingStore;
import com.ieltspath.assessment.config.SpeakingRecordingProperties;
import com.ieltspath.commonstorage.ObjectStorage;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class PurgeExpiredSpeakingRecordingsUseCaseTest {
    private final SpeakingRecordingStore recordings = mock(SpeakingRecordingStore.class);
    private final ObjectStorage storage = mock(ObjectStorage.class);
    private final PurgeExpiredSpeakingRecordingsUseCase useCase = new PurgeExpiredSpeakingRecordingsUseCase(
            recordings, storage, new SpeakingRecordingProperties("speaking-recordings", 90, 900));

    @Test
    void deletesDueObjectsMarksOnlySuccessfulDeletesAndContinuesAfterAnObjectFailure() {
        Instant now = Instant.parse("2026-10-31T00:00:00Z");
        UUID owner = UUID.randomUUID();
        var good = recording(owner, "webm");
        var failing = recording(owner, "wav");
        when(recordings.findExpired(now.minusSeconds(90L * 86_400), 100)).thenReturn(List.of(good, failing));
        doThrow(new IllegalStateException("storage unavailable")).when(storage).delete("speaking-recordings", failing.mediaReference());

        assertEquals(1, useCase.execute(now));

        verify(storage).delete("speaking-recordings", good.mediaReference());
        verify(recordings).markDeleted(good.id(), now);
        verify(recordings, never()).markDeleted(failing.id(), now);
    }

    @Test
    void doesNotProcessAnythingWhenThereAreNoExpiredRecordings() {
        Instant now = Instant.now();
        when(recordings.findExpired(any(), eq(100))).thenReturn(List.of());

        assertEquals(0, useCase.execute(now));
        verifyNoInteractions(storage);
    }

    private static SpeakingRecordingStore.Recording recording(UUID userId, String extension) {
        return new SpeakingRecordingStore.Recording(UUID.randomUUID(), userId,
                "speaking/" + userId + "/" + UUID.randomUUID() + "." + extension, null);
    }
}
