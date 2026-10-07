package com.ieltspath.assessment.domain.repository;

import com.ieltspath.assessment.domain.entity.AssessmentResult;

import java.util.Optional;
import java.util.UUID;

public interface AssessmentResultRepository {
    AssessmentResult save(AssessmentResult result);
    Optional<AssessmentResult> findLatestByAttemptId(UUID attemptId);

    /** The newest COMPLETED version, ignoring a newer version that is still being graded. */
    Optional<AssessmentResult> findLatestCompletedByAttemptId(UUID attemptId);

    Optional<AssessmentResult> findLatestForUpdateByAttemptId(UUID attemptId);

    Optional<AssessmentResult> findForUpdateById(UUID resultId);
}
