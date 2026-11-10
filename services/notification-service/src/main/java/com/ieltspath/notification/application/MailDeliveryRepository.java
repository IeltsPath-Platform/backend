package com.ieltspath.notification.application;

import java.time.Instant;
import java.util.UUID;

public interface MailDeliveryRepository {
    boolean exists(UUID eventId);
    void recordSent(UUID eventId, Instant sentAt);
}
