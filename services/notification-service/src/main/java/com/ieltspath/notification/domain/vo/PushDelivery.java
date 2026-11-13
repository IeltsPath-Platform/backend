package com.ieltspath.notification.domain.vo;

import java.util.UUID;

/**
 * A claimed push of one notification to one installation. {@code token} is null when the installation was revoked or
 * now belongs to another user, so the push must not be sent.
 */
public record PushDelivery(UUID id, int attempts, String token, UUID notificationId, String eventType, UUID postId,
                           UUID commentId, UUID gradingJobId) { }
