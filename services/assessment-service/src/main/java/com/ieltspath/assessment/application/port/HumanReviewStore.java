package com.ieltspath.assessment.application.port;

import com.fasterxml.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HumanReviewStore {
    Review create(CreateReview review);
    Optional<ReviewResult> find(UUID reviewId, UUID learnerId);

    /** Whether a review was already requested with this id, so a repeated request is not checked again. */
    boolean exists(UUID requestId);

    /** The grading job of a standalone review, to tell the examiner about it. */
    UUID jobIdOf(UUID reviewSubmissionId);
    /**
     * Stores the examiner's review and finishes the job. Returns who must be told when this call finished it, and empty
     * when the same review had already been stored (a repeated request), so the learner is told once.
     */
    Optional<Completed> complete(UUID jobId, UUID examinerId, BigDecimal band, JsonNode criteria, List<String> errorCodes,
                                 String feedback, JsonNode annotations);

    record Completed(UUID learnerId, UUID reviewSubmissionId, String skill) {}

    /** The essay of a standalone review job, for checking the errors an examiner marked in it. */
    Optional<String> essay(UUID jobId);

    /** Keeps the examiner's unfinished review. Only the examiner who claimed the job, and only until it is completed. */
    void saveDraft(UUID jobId, UUID examinerId, JsonNode payload);

    /** The unfinished review of the examiner who claimed the job; empty when none was saved. */
    Optional<Draft> draft(UUID jobId, UUID examinerId);

    record CreateReview(UUID requestId, UUID learnerId, UUID sourceSubmissionId, String source, String task,
                        JsonNode promptSnapshot, String essay, JsonNode rubric, UUID examinerId) {}
    record Review(UUID requestId, UUID reviewSubmissionId, String status) {}
    record ReviewResult(UUID reviewSubmissionId, String status, BigDecimal band, JsonNode criteria,
                        List<String> errorCodes, String feedback, JsonNode annotations, UUID examinerId) {}
    record Draft(JsonNode payload, Instant savedAt) {}
}
