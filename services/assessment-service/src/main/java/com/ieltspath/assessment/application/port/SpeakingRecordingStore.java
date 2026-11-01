package com.ieltspath.assessment.application.port;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpeakingRecordingStore {
    Optional<Recording> findById(UUID submissionId);
    boolean isAssignedExaminer(UUID submissionId, UUID examinerId);
    List<Recording> findExpired(Instant cutoff, int limit);
    void markDeleted(UUID submissionId, Instant deletedAt);

    record Recording(UUID id, UUID userId, String mediaReference, Instant recordingDeletedAt) { }
}
