package com.ieltspath.events.user;

import java.time.Instant;
import java.util.UUID;

/**
 * A Google account was linked to an existing account that had a password, and the password was removed. Emitted only
 * when a password was actually cleared, so the owner learns the account now signs in with Google.
 */
public record GoogleAccountLinkedV1(UUID eventId, String eventType, Instant occurredAt, UUID userId,
                                    String email, String displayName) {
    public static final String EVENT_TYPE = UserMessaging.GOOGLE_ACCOUNT_LINKED_ROUTING_KEY;
}
