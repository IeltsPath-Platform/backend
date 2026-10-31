package com.ieltspath.assessment.application.usecase;

import com.ieltspath.assessment.application.port.SpeakingRecordingStore;
import com.ieltspath.assessment.config.SpeakingRecordingProperties;
import com.ieltspath.assessment.domain.exception.AssessmentAccessDeniedException;
import com.ieltspath.assessment.domain.exception.AssessmentNotFoundException;
import com.ieltspath.commonstorage.ObjectStorage;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GetSpeakingRecordingUrlUseCaseTest {
    private final SpeakingRecordingStore recordings = mock(SpeakingRecordingStore.class);
    private final ObjectStorage storage = mock(ObjectStorage.class);
    private final UUID submissionId = UUID.randomUUID();
    private final UUID ownerId = UUID.randomUUID();
    private final UUID examinerId = UUID.randomUUID();
    private final String key = "speaking/" + ownerId + "/" + UUID.randomUUID() + ".webm";
    private final GetSpeakingRecordingUrlUseCase useCase = new GetSpeakingRecordingUrlUseCase(
            recordings, storage, new SpeakingRecordingProperties("speaking-recordings", 90, 900));

    @Test
    void signsForTheOwnerAndReturnsTheFifteenMinuteTtl() {
        when(recordings.findById(submissionId)).thenReturn(Optional.of(recording(null)));
        when(storage.signedDownloadUrl("speaking-recordings", key, Duration.ofSeconds(900))).thenReturn("signed-url");

        var result = useCase.execute(submissionId, ownerId);

        assertEquals("signed-url", result.url());
        assertEquals(900, result.expiresInSeconds());
        verify(recordings, never()).isAssignedExaminer(any(), any());
    }

    @Test
    void signsForTheExaminerWhoActuallyClaimedTheSubmission() {
        when(recordings.findById(submissionId)).thenReturn(Optional.of(recording(null)));
        when(recordings.isAssignedExaminer(submissionId, examinerId)).thenReturn(true);
        when(storage.signedDownloadUrl("speaking-recordings", key, Duration.ofSeconds(900))).thenReturn("signed-url");

        assertEquals("signed-url", useCase.execute(submissionId, examinerId).url());
        verify(recordings).isAssignedExaminer(submissionId, examinerId);
    }

    @Test
    void deniesOtherUsersAndDoesNotSignForAnUnassignedExaminer() {
        when(recordings.findById(submissionId)).thenReturn(Optional.of(recording(null)));
        when(recordings.isAssignedExaminer(submissionId, examinerId)).thenReturn(false);

        assertThrows(AssessmentAccessDeniedException.class, () -> useCase.execute(submissionId, examinerId));
        verifyNoInteractions(storage);
    }

    @Test
    void returnsNotFoundAfterTheRecordingHasBeenPurged() {
        when(recordings.findById(submissionId)).thenReturn(Optional.of(recording(Instant.now())));

        assertThrows(AssessmentNotFoundException.class, () -> useCase.execute(submissionId, ownerId));
        verifyNoInteractions(storage);
    }

    private SpeakingRecordingStore.Recording recording(Instant deletedAt) {
        return new SpeakingRecordingStore.Recording(submissionId, ownerId, key, deletedAt);
    }
}
