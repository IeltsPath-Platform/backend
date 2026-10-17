package com.ieltspath.learning.domain.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Points owed to learners, queued until the relay sends them to Access and Access reports back. */
public interface RewardGrantRepository {
    /** A grant waiting to be sent or to be settled; {@code attempts} counts the times it was sent. */
    record PendingGrant(UUID id, UUID userId, int amount, String reason, String referenceId, int attempts) {}

    /**
     * Queues a grant as pending. A grant already queued for the same reason and reference is not added again, so a
     * milestone reached twice pays once.
     */
    void enqueue(UUID userId, String reason, String referenceId, int amount);

    /**
     * Locks up to {@code limit} pending grants that are due: never sent, or last sent before {@code resendBefore}.
     * Rows another instance has locked are skipped.
     */
    List<PendingGrant> claimDue(int limit, Instant resendBefore);

    /** Records one more send. */
    void markSent(UUID id, Instant now);

    /** Gives up on a grant: it stays unpaid and the error is kept. */
    void markFailed(UUID id, String error, Instant now);

    /**
     * Records Access's answer ({@code GRANTED}, {@code CAPPED} or {@code FAILED}) for a pending grant. A grant already
     * settled is left as it is; returns whether this call changed it.
     */
    boolean settle(UUID id, String status, String error, Instant now);
}
