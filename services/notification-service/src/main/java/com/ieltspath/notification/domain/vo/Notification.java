package com.ieltspath.notification.domain.vo;

import java.time.Instant;
import java.util.UUID;

/**
 * One stored notification addressed to a single recipient. It refers to a community post or comment, or to a grading job
 * ({@code gradingJobId}); the other references are null.
 */
public record Notification(UUID id, UUID actorId, UUID recipientId, String eventType, UUID postId, UUID commentId,
                           UUID gradingJobId, Instant createdAt, Instant readAt) { }
