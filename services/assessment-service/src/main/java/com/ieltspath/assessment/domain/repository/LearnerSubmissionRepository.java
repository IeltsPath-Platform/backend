package com.ieltspath.assessment.domain.repository;

import com.ieltspath.assessment.domain.entity.LearnerSubmission;

import java.util.Optional;
import java.util.UUID;

public interface LearnerSubmissionRepository {
    LearnerSubmission save(LearnerSubmission submission);
    Optional<LearnerSubmission> findById(UUID id);
    Optional<LearnerSubmission> findByUserIdAndSubmissionKey(UUID userId, String submissionKey);
}
