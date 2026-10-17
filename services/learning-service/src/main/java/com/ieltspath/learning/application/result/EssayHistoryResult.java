package com.ieltspath.learning.application.result;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * The graded versions of one essay, oldest first. Versions that are not graded yet hold no grade and are left out. The
 * grading is automatic, so a delta also carries the grader's noise. {@code resolvedErrorCodes} and
 * {@code remainingErrorCodes} compare the two newest versions; {@code nextRevisionNo} is empty when the newest version
 * cannot be revised.
 */
public record EssayHistoryResult(UUID rootSubmissionId, List<Version> versions, List<String> resolvedErrorCodes,
                                 List<String> remainingErrorCodes, boolean canRevise, Integer nextRevisionNo,
                                 UUID reviseFromSubmissionId) {
    /** {@code deltaOverall} and each criterion's delta are against the version before; null for the first. */
    public record Version(UUID submissionId, int revisionNo, int wordCount, BigDecimal overallBand,
                          BigDecimal deltaOverall, List<CriterionBand> criteria, List<String> errorCodes) {}

    public record CriterionBand(String code, BigDecimal band, BigDecimal delta) {}
}
