package com.ieltspath.learning.application.port;

import java.util.Optional;
import java.util.UUID;

/** The result version applied per attempt, so a redelivered or older AssessmentCompleted event is ignored. */
public interface AssessmentResultLog {
    Optional<Integer> appliedVersion(UUID userId, UUID attemptId);

    /** Keeps the highest version. */
    void recordVersion(UUID userId, UUID attemptId, int resultVersion);
}
