package com.ieltspath.events.library;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * A learner has saved vocabulary that is due for review. Emitted at most once per learner and {@code reviewDate}: the event
 * id is derived from both, so a rerun of the daily scan produces the same id and is dropped by the outbox and by Notification.
 * {@code dueCount} is the number of previously learned words due at {@code occurredAt}; it explains why the event was
 * emitted and is not shown to the learner.
 */
public record VocabularyReviewDueV1(UUID eventId, String eventType, Instant occurredAt, UUID userId,
                                    LocalDate reviewDate, int dueCount) {
    public static final String EVENT_TYPE = "library.vocabulary-review-due.v1";
}
