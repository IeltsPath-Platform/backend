package com.ieltspath.events.user;

import java.time.Instant;
import java.util.UUID;

/** Password reset mail request; the raw token is transient and expires with the account-action token. */
public record PasswordResetRequestedV1(UUID eventId, String eventType, Instant occurredAt, UUID userId,
                                       String email, String displayName, String resetToken, Instant expiresAt) {
    public static final String EVENT_TYPE = UserMessaging.PASSWORD_RESET_REQUESTED_ROUTING_KEY;
}
