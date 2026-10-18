package com.ieltspath.learning.application.port;

import java.util.UUID;

/**
 * Access Service point operations, made with the learner's own bearer token so Access can check that the debited
 * user is the caller. Failures are thrown as {@link com.ieltspath.learning.application.exception.InsufficientPointsException}
 * or {@link com.ieltspath.learning.application.exception.AccessUnavailableException}.
 */
public interface AccessClient {
    long balance();

    /** Whether the learner has an active subscription (PREMIUM) right now. */
    boolean premium();

    /** Idempotent by {@code idempotencyKey}; a resend returns the original ledger entry. */
    default UUID debit(UUID userId, int amount, UUID referenceId, String idempotencyKey, String description) {
        return debit(userId, amount, "LESSON_WRITING", referenceId, idempotencyKey, description);
    }

    /** Debit with the reference type Access uses to tell the purpose apart, such as {@code HEART_REFILL}. */
    UUID debit(UUID userId, int amount, String referenceType, UUID referenceId, String idempotencyKey,
               String description);
}
