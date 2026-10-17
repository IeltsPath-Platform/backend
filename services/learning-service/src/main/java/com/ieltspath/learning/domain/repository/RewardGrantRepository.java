package com.ieltspath.learning.domain.repository;

import java.util.UUID;

/** Points owed to learners, queued until the relay sends them to Access. */
public interface RewardGrantRepository {
    /**
     * Queues a grant as pending. A grant already queued for the same reason and reference is not added again, so a
     * milestone reached twice pays once.
     */
    void enqueue(UUID userId, String reason, String referenceId, int amount);
}
