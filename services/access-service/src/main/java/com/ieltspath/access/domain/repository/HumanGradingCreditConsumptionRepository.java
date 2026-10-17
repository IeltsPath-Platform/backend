package com.ieltspath.access.domain.repository;

import com.ieltspath.access.domain.entity.HumanGradingCreditConsumption;

import java.util.Optional;
import java.util.UUID;

public interface HumanGradingCreditConsumptionRepository {
    void lockSubmission(UUID submissionId);
    Optional<HumanGradingCreditConsumption> findBySubmissionId(UUID submissionId);
    void save(HumanGradingCreditConsumption consumption);
    void markRefunded(UUID submissionId, int remainingCredits);
}
