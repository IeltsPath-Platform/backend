package com.group01.assessment.domain.repository;

import com.group01.assessment.domain.entity.OutboxEvent;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository {
    OutboxEvent save(OutboxEvent event);

    /**
     * Locks up to {@code limit} committed, unpublished events for the caller's transaction. Rows locked by
     * another relay instance are skipped, and events that exhausted {@code maxAttempts} stay parked.
     */
    List<OutboxEvent> claimUnpublished(int limit, int maxAttempts);

    void markPublished(UUID id, Instant publishedAt);

    void recordFailure(UUID id, String error);
}
