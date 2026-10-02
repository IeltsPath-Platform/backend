package com.group01.learning.domain.vo;

/**
 * GRADING while the LLM grades; PAYMENT_PENDING once graded until points are debited; GRADED after the debit, final;
 * FAILED when grading did not finish (it may be graded again on the same essay).
 */
public enum WritingSubmissionStatus {
    GRADING,
    PAYMENT_PENDING,
    GRADED,
    FAILED
}
