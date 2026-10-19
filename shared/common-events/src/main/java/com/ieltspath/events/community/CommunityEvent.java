package com.ieltspath.events.community;

import java.time.Instant;
import java.util.UUID;

/** Community's interaction announcement consumed by Notification; see docs/contracts/community-events-v1.md. */
public record CommunityEvent(UUID eventId, String eventType, Instant occurredAt, UUID actorId,
                             UUID recipientId, UUID postId, UUID commentId) { }
