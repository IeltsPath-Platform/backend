package com.ieltspath.learning.application.port;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Durable queue and outbox boundary for placement Speaking grading. */
public interface PlacementSpeakingJobStore {
    record Job(UUID eventId, UUID attemptId, UUID userId, String payload, int attempts,
               String correlationId, String traceContext) { }
    /** What the learner may read of a job; {@code result} is JSON and null unless graded. */
    record Stored(String status, String result) { }
    record OutboxEvent(UUID id, String eventType, String payload, Instant createdAt,
                       String correlationId, String traceContext) { }

    boolean enqueue(UUID eventId, UUID attemptId, UUID userId, String payload, String correlationId, String traceContext);
    List<Job> claimDue(int limit, int maxAttempts, Instant now, Instant staleBefore);
    void retry(UUID eventId, String failureCode, Instant nextAttempt);
    /** @param result the graded result as JSON, or null when grading failed */
    void complete(UUID eventId, String status, String failureCode, String result, OutboxEvent event);
    Optional<Stored> findOwned(UUID attemptId, UUID userId);
    List<OutboxEvent> claimOutbox(int limit);
    void markPublished(UUID id);
    void recordPublishFailure(UUID id, String message);
}
