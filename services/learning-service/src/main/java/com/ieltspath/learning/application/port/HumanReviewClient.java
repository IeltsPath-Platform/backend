package com.ieltspath.learning.application.port;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Mentor review of an essay: the human-grading credit (Access, with the learner's own bearer token) and the independent
 * review (Assessment). Failures are thrown as
 * {@link com.ieltspath.learning.application.exception.LearningRequestException}: {@code 402} when the learner has no
 * credit, {@code 409} for a credit conflict, {@code 503} when a service cannot be reached.
 */
public interface HumanReviewClient {
    /**
     * Spends one credit for the essay; idempotent by {@code submissionId}. {@code charged} tells whether this call spent
     * it: only a call that did may give it back.
     */
    CreditCharge consumeCredit(UUID submissionId);

    /** Gives the credit of an essay back, for a review that could not be created; idempotent. */
    void refundCredit(UUID submissionId);

    /**
     * Idempotent by {@code requestId}; returns the id of the independent review submission. A review that Assessment
     * refused outright fails with code {@code HUMAN_GRADING_CONFLICT}, {@code EXAMINER_UNAVAILABLE} (the chosen examiner
     * is full, not accepting or unknown) or {@code HUMAN_REVIEW_REJECTED}: nothing was created. Any other failure (timeout, server error, no connection) may have left a review behind.
     */
    UUID createReview(CreateReview request);

    /** The review as the learner may see it; grade fields are null while it is PENDING or CANCELLED. */
    Review fetchReview(UUID reviewSubmissionId, UUID learnerId);

    /**
     * Gives up a late review whose examiner can no longer be changed; idempotent. Fails with {@code
     * REVIEW_CANCEL_NOT_ALLOWED} (409) when it is not late yet or an examiner change is left, and with {@code
     * HUMAN_GRADING_CONFLICT} (409) when the examiner graded it in the meantime.
     */
    Review cancelReview(UUID reviewSubmissionId);

    record CreditCharge(int remainingCredits, boolean charged) {}

    /**
     * {@code examinerId} is the examiner the learner chose; only that examiner receives the essay. {@code images} are the
     * chart images of the prompt the learner saw, so the examiner reads the essay against the same Task 1 chart.
     */
    record CreateReview(UUID requestId, UUID learnerId, UUID sourceSubmissionId, String source, String task,
                        String promptStem, List<PromptImage> images, String essay, String rubricCode, Integer rubricVersion,
                        UUID examinerId) {
        public CreateReview {
            images = images == null ? List.of() : List.copyOf(images);
        }
    }

    record PromptImage(String url, String altText) {}

    /** {@code examinerId} is who the learner chose; the learner looks the examiner's profile up in User Service. */
    record Review(UUID reviewSubmissionId, String status, BigDecimal band, List<Criterion> criteria,
                  List<String> errorCodes, String feedback, List<Annotation> annotations, UUID examinerId) {}

    record Criterion(String criterion, BigDecimal band, String comment) {}

    /**
     * An error the examiner marked in the essay: {@code start} and {@code end} are UTF-16 offsets, {@code end}
     * exclusive. {@code errorCode} is absent while the code is still in review; the excerpt and suggestion remain.
     */
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    record Annotation(String excerpt, Integer start, Integer end, String errorCode, String suggestion) {}
}
