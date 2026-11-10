package com.ieltspath.user.domain.repository;

import com.ieltspath.user.domain.aggregate.OutboxEvent;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository {
    OutboxEvent save(OutboxEvent event);
    List<OutboxEvent> claimPending(int limit, int maxAttempts);
    void markPublished(UUID id, Instant at);
    void recordFailure(UUID id, String errorType, int maxAttempts);
    int clearExpiredFailedPayloads(Instant olderThan);
}
