package com.ieltspath.events.user;

import java.time.Instant;
import java.util.UUID;

/**
 * An account's email-verification state and contact details. Published when an account is created (unverified), when it
 * is verified, when a reset proves the address, and once per account by the backfill. Consumers keep the latest state by
 * {@code occurredAt}. The contact fields let other services address notices without calling user-service.
 */
public record UserEmailVerificationStateV1(UUID eventId, String eventType, Instant occurredAt, UUID userId,
                                           boolean verified, String email, String displayName) {
    public static final String EVENT_TYPE = UserMessaging.EMAIL_VERIFICATION_STATE_ROUTING_KEY;
}
