package com.ieltspath.events.user;

import java.time.Instant;
import java.util.UUID;

public record GoogleAccountUnlinkedV1(UUID eventId, String eventType, Instant occurredAt, UUID userId,
                                      String email, String displayName) {
    public static final String EVENT_TYPE = UserMessaging.GOOGLE_ACCOUNT_UNLINKED_ROUTING_KEY;
}
