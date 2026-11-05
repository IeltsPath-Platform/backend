package com.ieltspath.events.assessment;

import java.time.Instant;
import java.util.UUID;

/**
 * Assessment tells Notification that a learner chose this examiner for a job, so the examiner knows at once that there is
 * something to grade; see docs/contracts/grading-requested-v1.md. {@code gradingJobId} is the examiner's job in their own
 * queue; {@code deadline} is the moment it becomes late. Grading is blind: the event carries no learner, essay text or band.
 */
public record GradingRequestedV1(UUID eventId, String eventType, Instant occurredAt, UUID examinerId, UUID gradingJobId,
                                 String skill, Instant deadline) {
    public static final String EVENT_TYPE = "assessment.grading-requested.v1";
}
