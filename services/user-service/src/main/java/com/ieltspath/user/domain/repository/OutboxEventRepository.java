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
    /**
     * Drops the raw link token from reset and verification events that outlived it, whether they failed, are stuck pending
     * or never reached the broker, and marks them failed. Safe to run from several instances: it is one statement.
     */
    int expireLinkPayloads(Instant resetBefore, Instant verificationBefore);
}
