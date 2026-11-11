package com.ieltspath.events.user;

import java.time.Instant;
import java.util.UUID;

/**
 * Asks Notification to email the verification link to a newly registered account. The raw token is transient: it is
 * stored only as a hash and expires with the account-action token.
 */
public record EmailVerificationRequestedV1(UUID eventId, String eventType, Instant occurredAt, UUID userId,
                                           String email, String displayName, String verificationToken, Instant expiresAt) {
    public static final String EVENT_TYPE = UserMessaging.EMAIL_VERIFICATION_REQUESTED_ROUTING_KEY;
}
