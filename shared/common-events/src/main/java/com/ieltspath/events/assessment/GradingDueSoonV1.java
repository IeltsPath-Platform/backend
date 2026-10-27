package com.ieltspath.events.assessment;

import java.time.Instant;
import java.util.UUID;

/**
 * Assessment tells Notification that a job the examiner was chosen for will be late soon, so the examiner can give it
 * priority; see docs/contracts/grading-due-soon-v1.md. {@code gradingJobId} is the examiner's job in their own queue;
 * {@code deadline} is the moment it becomes late. Grading is blind: the event carries no learner, essay text or band.
 */
public record GradingDueSoonV1(UUID eventId, String eventType, Instant occurredAt, UUID examinerId, UUID gradingJobId,
                               String skill, Instant deadline) {
    public static final String EVENT_TYPE = "assessment.grading-due-soon.v1";
}
