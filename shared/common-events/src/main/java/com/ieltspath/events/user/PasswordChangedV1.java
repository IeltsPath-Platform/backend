package com.ieltspath.events.user;

import java.time.Instant;
import java.util.UUID;

/**
 * The account's password was changed, either by the user or by a completed reset. Carries no secret: no password, hash,
 * token or IP address. Sent to the address on the account so the owner can tell if the change was not theirs.
 */
public record PasswordChangedV1(UUID eventId, String eventType, Instant occurredAt, UUID userId,
                                String email, String displayName) {
    public static final String EVENT_TYPE = UserMessaging.PASSWORD_CHANGED_ROUTING_KEY;
}
