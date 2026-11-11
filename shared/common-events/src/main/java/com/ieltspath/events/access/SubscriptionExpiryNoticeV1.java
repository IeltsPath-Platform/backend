package com.ieltspath.events.access;

import java.time.Instant;
import java.util.UUID;

/**
 * A Premium subscription is about to end ({@code SEVEN_DAYS}, {@code ONE_DAY}) or has ended ({@code EXPIRED}). Carries the
 * address and name to write to, so Notification does not call Access or User. Emitted once per subscription and stage.
 */
public record SubscriptionExpiryNoticeV1(UUID eventId, String eventType, Instant occurredAt, UUID userId,
                                         UUID subscriptionId, String stage, Instant endsAt,
                                         String email, String displayName) {
    public static final String EVENT_TYPE = "access.subscription-expiry-notice.v1";
}
