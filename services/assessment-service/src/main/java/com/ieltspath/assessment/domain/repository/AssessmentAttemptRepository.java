package com.ieltspath.assessment.domain.repository;

import com.ieltspath.assessment.domain.aggregate.AssessmentAttempt;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AssessmentAttemptRepository {
    AssessmentAttempt save(AssessmentAttempt attempt);
    Optional<AssessmentAttempt> findById(UUID id);
    Optional<AssessmentAttempt> findByIdAndUserId(UUID id, UUID userId);
    List<AssessmentAttempt> findByUserId(UUID userId);
}
