package com.ieltspath.learning.application.result;

import com.ieltspath.learning.domain.vo.WritingGrade;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * A learner's view of one essay. The grade, points and sample answer are set only when {@code status} is GRADED;
 * {@code code} (PAYMENT_PENDING) or {@code failureCode} (GRADING, FAILED) explain the other states.
 */
public record WritingSubmissionResult(UUID submissionId, String status, String task, Integer wordCount,
                                      BigDecimal overallBand, Boolean passed, WritingGrade grade,
                                      Integer pointsCharged, String sampleAnswer, String code, String failureCode) {
}
