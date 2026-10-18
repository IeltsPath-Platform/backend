package com.ieltspath.assessment.application.port;

import java.util.UUID;

/** The human-grading credits of Access, spent with the bearer token of the learner who asks. */
public interface HumanGradingCreditPort {
    /** Spends the submission credit; true when this call spent it, false when it had been spent before. */
    boolean consume(UUID submissionId);

    /** Gives the credit of the submission back; idempotent. */
    void refund(UUID submissionId);
}
