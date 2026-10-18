package com.ieltspath.notification.domain.vo;

import java.time.Instant;
import java.util.UUID;

/** One stored notification addressed to a single recipient. */
public record Notification(UUID id, UUID actorId, UUID recipientId, String eventType, UUID postId, UUID commentId,
                           Instant createdAt, Instant readAt) { }
