package com.ieltspath.access.domain.repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** Latest email-verification state and contact details per account, as reported by user-service. */
public interface UserEmailVerificationRepository {
    /** Stores the state only if it is newer than the one already held. */
    void recordState(UUID userId, boolean verified, String email, String displayName, Instant occurredAt);

    /**
     * True only when the account has been reported as verified. An account with no state yet is not verified: the
     * gate fails closed, so the verification-state backfill must run before the gate is enabled in an environment.
     */
    boolean isVerified(UUID userId);

    /** Address and name to write to, when user-service has reported them. */
    Optional<Contact> findContact(UUID userId);

    record Contact(String email, String displayName) { }
}
