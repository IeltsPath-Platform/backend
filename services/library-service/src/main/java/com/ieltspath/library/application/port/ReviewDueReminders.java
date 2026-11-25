package com.ieltspath.library.application.port;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Finds learners who have saved words due for review and queues the reminder event for them. */
public interface ReviewDueReminders {

    record DueLearner(UUID userId, int dueCount) { }

    /**
     * Learners with at least {@code minCount} previously learned words due at {@code now}, ordered by user id and
     * starting after {@code afterUserId}, at most {@code limit} of them. New, never-reviewed words are not counted.
     */
    List<DueLearner> findDueLearners(Instant now, int minCount, UUID afterUserId, int limit);

    /** Queues the event in the outbox. Returns false when an event with this id is already queued. */
    boolean queue(UUID eventId, UUID userId, String payload, Instant createdAt);
}
