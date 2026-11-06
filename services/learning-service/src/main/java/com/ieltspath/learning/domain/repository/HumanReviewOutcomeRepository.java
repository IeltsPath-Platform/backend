package com.ieltspath.learning.domain.repository;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

/** Which examiner reviews were applied to mastery, and the study points each one produced. */
public interface HumanReviewOutcomeRepository {
    /**
     * Records that the review was applied; false when it already was. Called under the learner's lock before any evidence is
     * written, so a repeated event changes nothing.
     */
    boolean insertIfAbsent(UUID reviewSubmissionId, UUID userId, UUID submissionId, String studyPointsJson,
                           BigDecimal overallBand, boolean passed);

    /** The examiner's result for the essay: its overall band and whether it passed; empty when no review was applied. */
    Optional<ExaminerResult> resultOf(UUID submissionId);

    /** The examiner's overall band of an essay and whether that band reached the essay's pass band. */
    record ExaminerResult(BigDecimal overallBand, boolean passed) { }

    /** The study points JSON of the essay's applied review; empty when no review was applied to the essay. */
    Optional<String> studyPointsOf(UUID submissionId);
}
