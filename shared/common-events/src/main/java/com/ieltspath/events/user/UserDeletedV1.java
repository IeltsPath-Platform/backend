package com.ieltspath.events.user;

import java.time.Instant;
import java.util.UUID;

/**
 * Emitted when a user permanently deletes their own account. Carries no PII; consumers use {@code eventId} for
 * idempotency and {@code userId} to erase or anonymize owned data and record a tombstone.
 */
public record UserDeletedV1(UUID eventId, String eventType, Instant occurredAt, UUID userId) {
    public static final String EVENT_TYPE = UserMessaging.USER_DELETED_ROUTING_KEY;
}
