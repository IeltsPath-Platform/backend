package com.ieltspath.assessment.application.port;

import com.ieltspath.events.assessment.SpeakingGradingCompletedV1;
import com.ieltspath.events.assessment.SpeakingGradingRequestedV1;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Persistence boundary for asynchronous placement Speaking grades. */
public interface PlacementSpeakingGradeStore {
    record RecordingItem(UUID attemptItemId, int partNo, int questionNo, String question, String objectKey) { }
    record Grade(String status, Double band, String feedback, String rubricCode, Integer rubricVersion,
                 String failureCode) { }

    List<RecordingItem> recordings(UUID attemptId, UUID userId);
    void enqueue(SpeakingGradingRequestedV1 event, Instant deadline);
    boolean complete(SpeakingGradingCompletedV1 event);
    Optional<Grade> find(UUID attemptId);
    List<UUID> failExpired(Instant now, int limit);
}
