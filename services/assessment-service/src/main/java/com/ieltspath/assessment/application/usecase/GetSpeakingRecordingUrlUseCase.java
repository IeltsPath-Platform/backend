package com.ieltspath.assessment.application.usecase;

import com.ieltspath.assessment.application.port.SpeakingRecordingStore;
import com.ieltspath.assessment.config.SpeakingRecordingProperties;
import com.ieltspath.assessment.domain.exception.AssessmentAccessDeniedException;
import com.ieltspath.assessment.domain.exception.AssessmentNotFoundException;
import com.ieltspath.commonstorage.ObjectStorage;
import com.ieltspath.commonstorage.key.InvalidObjectKeyException;
import com.ieltspath.commonstorage.key.ObjectKeys;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
public class GetSpeakingRecordingUrlUseCase {
    private final SpeakingRecordingStore recordings;
    private final ObjectStorage storage;
    private final SpeakingRecordingProperties properties;

    public GetSpeakingRecordingUrlUseCase(SpeakingRecordingStore recordings, @Lazy ObjectStorage storage,
                                          SpeakingRecordingProperties properties) {
        this.recordings = recordings;
        this.storage = storage;
        this.properties = properties;
    }

    public RecordingUrlResult execute(UUID submissionId, UUID callerId) {
        var recording = recordings.findById(submissionId)
                .orElseThrow(() -> new AssessmentNotFoundException("Speaking recording not found"));
        if (!recording.userId().equals(callerId) && !recordings.isAssignedExaminer(submissionId, callerId)) {
            throw new AssessmentAccessDeniedException();
        }
        if (recording.recordingDeletedAt() != null || recording.mediaReference() == null) {
            throw new AssessmentNotFoundException("Speaking recording not found");
        }
        try {
            ObjectKeys.requireOwnedBy(recording.mediaReference(), "speaking", recording.userId());
        } catch (InvalidObjectKeyException invalidKey) {
            throw new AssessmentNotFoundException("Speaking recording not found");
        }
        String url = storage.signedDownloadUrl(properties.getBucket(), recording.mediaReference(),
                Duration.ofSeconds(properties.getRecordingUrlTtlSeconds()));
        return new RecordingUrlResult(url, properties.getRecordingUrlTtlSeconds());
    }

    public record RecordingUrlResult(String url, int expiresInSeconds) { }
}
