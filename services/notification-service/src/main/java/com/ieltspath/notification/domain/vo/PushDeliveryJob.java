package com.ieltspath.notification.domain.vo;

import java.util.UUID;
import java.time.Instant;

public record PushDeliveryJob(UUID id, UUID notificationId, UUID recipientId, UUID deviceId,
                              long registrationVersion, int attemptCount, UUID claimId, Instant expiresAt) { }
