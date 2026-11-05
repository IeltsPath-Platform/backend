package com.ieltspath.learning.domain.vo;

/**
 * GRADING while the LLM grades; PAYMENT_PENDING once graded until points are debited; GRADED after the debit, final;
 * FAILED when grading did not finish (it may be graded again on the same essay); AWAITING_EXAMINER when the learner sent
 * the essay to an examiner instead of the AI (the examiner's review is read from Assessment).
 */
public enum WritingSubmissionStatus {
    GRADING,
    PAYMENT_PENDING,
    GRADED,
    FAILED,
    AWAITING_EXAMINER
}
