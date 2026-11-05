package com.ieltspath.events.assessment;

import java.time.Instant;
import java.util.UUID;

/**
 * Assessment tells Notification that the examiner a learner chose has not graded within the allowed time, so the learner
 * can choose someone else; see docs/contracts/grading-overdue-v1.md. {@code gradingJobId} is the learner's job to look up
 * in Assessment ({@code GET /api/assessments/grading-jobs/mine}); {@code deadline} is the moment it became late. The
 * event carries no essay text, band or learner name.
 */
public record GradingOverdueV1(UUID eventId, String eventType, Instant occurredAt, UUID learnerId, UUID examinerId,
                               UUID gradingJobId, String skill, Instant deadline) {
    public static final String EVENT_TYPE = "assessment.grading-overdue.v1";
}
