package com.ieltspath.access.domain.repository;

import com.ieltspath.access.domain.entity.OutboxEvent;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OutboxEventRepository {

    Optional<OutboxEvent> findById(UUID id);

    List<OutboxEvent> findPendingEvents(int limit);

    /** Locks up to {@code limit} pending rows with attempts left; rows another relay holds are skipped. */
    List<OutboxEvent> claimPending(int limit, int maxAttempts);

    void markPublished(UUID id, Instant publishedAt);

    /** Counts one failed attempt; the row becomes FAILED once it reaches {@code maxAttempts}. */
    void recordFailure(UUID id, String error, int maxAttempts);

    OutboxEvent save(OutboxEvent event);
}
