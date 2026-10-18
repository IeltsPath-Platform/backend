package com.ieltspath.access.domain.repository;

import com.ieltspath.access.domain.entity.HumanGradingCreditConsumption;

import java.util.Optional;
import java.util.UUID;

public interface HumanGradingCreditConsumptionRepository {
    void lockSubmission(UUID submissionId);
    Optional<HumanGradingCreditConsumption> findBySubmissionId(UUID submissionId);
    void save(HumanGradingCreditConsumption consumption);
    void markRefunded(UUID submissionId, int remainingCredits);
    /** Charges a submission whose earlier credit was refunded: clears the refund and records the new debit. */
    void markReconsumed(UUID submissionId, UUID subscriptionId, int remainingCredits);
}
