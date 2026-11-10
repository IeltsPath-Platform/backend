package com.ieltspath.notification.application;

import java.time.Instant;
import java.util.UUID;

/** Records which template was sent for which source event, so a redelivered event does not send the same mail twice. */
public interface MailDeliveryRepository {
    boolean exists(UUID eventId, String template);
    void recordSent(UUID eventId, String template, Instant sentAt);
}
