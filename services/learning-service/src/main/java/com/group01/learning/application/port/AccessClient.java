package com.group01.learning.application.port;

import java.util.UUID;

/**
 * Access Service point operations, made with the learner's own bearer token so Access can check that the debited
 * user is the caller. Failures are thrown as {@link com.group01.learning.application.exception.InsufficientPointsException}
 * or {@link com.group01.learning.application.exception.AccessUnavailableException}.
 */
public interface AccessClient {
    long balance();

    /** Idempotent by {@code idempotencyKey}; a resend returns the original ledger entry. */
    UUID debit(UUID userId, int amount, UUID referenceId, String idempotencyKey, String description);
}
