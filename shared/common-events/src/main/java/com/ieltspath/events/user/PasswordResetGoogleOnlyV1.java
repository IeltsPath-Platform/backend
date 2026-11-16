package com.ieltspath.events.user;

import java.time.Instant;
import java.util.UUID;

/**
 * Someone asked to reset the password of an account that only signs in with Google and never had a password. No reset link
 * exists for it; the owner is told to use Google instead. Carries the address and name to write to, so Notification does
 * not call User.
 */
public record PasswordResetGoogleOnlyV1(UUID eventId, String eventType, Instant occurredAt, UUID userId,
                                        String email, String displayName) {
    public static final String EVENT_TYPE = UserMessaging.PASSWORD_RESET_GOOGLE_ONLY_ROUTING_KEY;
}
