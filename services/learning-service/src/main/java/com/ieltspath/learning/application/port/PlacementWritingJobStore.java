package com.ieltspath.learning.application.port;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Durable queue of placement essay grading; its result events leave through the placement grading outbox. */
public interface PlacementWritingJobStore {
    boolean enqueue(UUID eventId, UUID attemptId, UUID userId, String payload, String correlationId, String traceContext);
    List<PlacementSpeakingJobStore.Job> claimDue(int limit, int maxAttempts, Instant now, Instant staleBefore);
    void retry(UUID eventId, String failureCode, Instant nextAttempt);
    /** @param result every essay with its grade, as JSON */
    void complete(UUID eventId, String status, String failureCode, String result,
                  PlacementSpeakingJobStore.OutboxEvent event);
    Optional<PlacementSpeakingJobStore.Stored> findOwned(UUID attemptId, UUID userId);
}
