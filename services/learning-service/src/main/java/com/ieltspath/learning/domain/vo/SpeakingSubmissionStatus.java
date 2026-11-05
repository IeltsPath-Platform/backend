package com.ieltspath.learning.domain.vo;

/**
 * UPLOADING while the recordings are being stored; QUEUED once all are in and the submission waits for analysis;
 * ANALYZING and GRADING while the speech service and the LLM work; PAYMENT_PENDING once graded until the points are
 * debited; GRADED after the debit, final. FAILED_RETRYABLE, NEEDS_EXAMINER and ABANDONED end a submission without a grade.
 */
public enum SpeakingSubmissionStatus {
    UPLOADING,
    QUEUED,
    ANALYZING,
    GRADING,
    PAYMENT_PENDING,
    GRADED,
    FAILED_RETRYABLE,
    NEEDS_EXAMINER,
    ABANDONED;

    /** Whether the submission still waits for a grade; a learner may have only one of these. */
    public boolean isActive() {
        return this == UPLOADING || this == QUEUED || this == ANALYZING || this == GRADING
                || this == PAYMENT_PENDING || this == FAILED_RETRYABLE;
    }
}
