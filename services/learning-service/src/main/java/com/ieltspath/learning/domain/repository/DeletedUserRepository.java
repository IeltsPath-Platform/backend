package com.ieltspath.learning.domain.repository;

import java.time.Instant;
import java.util.UUID;

/** Tracks deleted accounts to ignore late events and schedule batched data cleanup. */
public interface DeletedUserRepository {
    boolean isDeleted(UUID userId);

    void recordDeletion(UUID eventId, UUID userId, Instant occurredAt);
}
