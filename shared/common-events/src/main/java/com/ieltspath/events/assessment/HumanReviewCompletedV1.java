package com.ieltspath.events.assessment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Assessment tells Learning what an examiner graded in a standalone Writing review: the band of each criterion and the
 * error codes the examiner recorded, with how often each appeared. The event carries no essay, comment, feedback, excerpt
 * or suggestion, and no examiner identity; Learning reads the essay's own task and source from its stored submission.
 * Written once, in the transaction that stores the review (see docs/contracts/human-review-completed-v1.md).
 */
public record HumanReviewCompletedV1(UUID eventId, String eventType, Instant occurredAt, UUID learnerId,
                                     UUID reviewSubmissionId, UUID sourceSubmissionId, List<Criterion> criteria,
                                     List<ErrorCodeCount> errorCodes) {
    public static final String EVENT_TYPE = "assessment.human-review-completed.v1";

    /** One criterion as the examiner named it ({@code taskResponse}, {@code coherence}, {@code lexical}, {@code grammar}). */
    public record Criterion(String criterion, BigDecimal band) { }

    /** An error code and how many times the examiner marked it; at least 1. */
    public record ErrorCodeCount(String code, int count) { }
}
