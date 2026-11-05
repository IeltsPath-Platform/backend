package com.ieltspath.events.assessment;

import java.time.Instant;
import java.util.UUID;

/**
 * Assessment tells Notification that the examiner finished grading a learner's essay, so the learner can read the comments
 * and the marked errors; see docs/contracts/grading-completed-v1.md. {@code gradingJobId} is the learner's own job
 * ({@code GET /api/assessments/grading-jobs/mine}) and {@code reviewSubmissionId} the review Learning holds for the essay.
 * The notice carries no band, comment or essay text: the learner reads those in the app.
 */
public record GradingCompletedV1(UUID eventId, String eventType, Instant occurredAt, UUID learnerId, UUID gradingJobId,
                                 UUID reviewSubmissionId, String skill) {
    public static final String EVENT_TYPE = "assessment.grading-completed.v1";
}
