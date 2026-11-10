package com.ieltspath.notification.application;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Records which template was sent for which source event, so a redelivered event does not send the same mail twice. */
public interface MailDeliveryRepository {
    boolean exists(UUID eventId, String template);
    void recordSent(UUID eventId, String template, Instant sentAt);

    /** Reserves one of the three daily learning/reminder mail slots for a user. */
    boolean reserveDailyNonSecuritySlot(UUID userId, LocalDate date, UUID eventId, String template, Instant reservedAt);

    void releaseDailyNonSecuritySlot(UUID userId, LocalDate date, UUID eventId, String template);
}
