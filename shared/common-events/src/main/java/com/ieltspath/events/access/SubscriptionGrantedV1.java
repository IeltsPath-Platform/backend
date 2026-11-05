package com.ieltspath.events.access;

import java.time.Instant;
import java.util.UUID;

/**
 * Access tells Notification that an administrator granted a Premium subscription to a learner; see
 * docs/contracts/subscription-granted-v1.md. {@code endsAt} is when the granted access ends.
 */
public record SubscriptionGrantedV1(UUID eventId, String eventType, Instant occurredAt, UUID userId, UUID subscriptionId,
                                    String planCode, Instant endsAt) {
    public static final String EVENT_TYPE = "access.subscription-granted.v1";
}
