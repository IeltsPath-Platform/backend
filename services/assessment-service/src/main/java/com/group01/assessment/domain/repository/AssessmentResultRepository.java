package com.group01.assessment.domain.repository;

import com.group01.assessment.domain.entity.AssessmentResult;

import java.util.Optional;
import java.util.UUID;

public interface AssessmentResultRepository {
    AssessmentResult save(AssessmentResult result);
    Optional<AssessmentResult> findLatestByAttemptId(UUID attemptId);

    Optional<AssessmentResult> findLatestForUpdateByAttemptId(UUID attemptId);

    Optional<AssessmentResult> findForUpdateById(UUID resultId);
}
