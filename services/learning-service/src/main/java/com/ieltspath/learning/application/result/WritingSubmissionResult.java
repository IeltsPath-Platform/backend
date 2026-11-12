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
                                      Integer pointsCharged, String sampleAnswer, String code, String failureCode,
                                      String essayText, Revision revision) {
    /** For a revision: the version it follows and how its error kinds compare with that version. */
    public record Revision(UUID previousSubmissionId, java.util.List<String> fixedCodes,
                           java.util.List<String> repeatedCodes, java.util.List<String> newCodes) {}

    public WritingSubmissionResult(UUID submissionId, String status, String task, Integer wordCount,
                                   BigDecimal overallBand, Boolean passed, WritingGrade grade, Integer pointsCharged,
                                   String sampleAnswer, String code, String failureCode, String essayText) {
        this(submissionId, status, task, wordCount, overallBand, passed, grade, pointsCharged, sampleAnswer, code,
                failureCode, essayText, null);
    }

    public WritingSubmissionResult(UUID submissionId, String status, String task, Integer wordCount,
                                   BigDecimal overallBand, Boolean passed, WritingGrade grade, Integer pointsCharged,
                                   String sampleAnswer, String code, String failureCode) {
        this(submissionId, status, task, wordCount, overallBand, passed, grade, pointsCharged, sampleAnswer, code,
                failureCode, null);
    }
}
