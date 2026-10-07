package com.ieltspath.assessment.application.result;

import com.ieltspath.assessment.domain.aggregate.AssessmentAttempt;
import com.ieltspath.assessment.domain.vo.*;

import java.time.Instant;
import java.util.UUID;

public record AssessmentAttemptResult(UUID id, UUID userId, UUID packageVersionId, AttemptType attemptType,
                                      AttemptMode mode, AttemptChannel channel, AttemptStatus status,
                                      Instant startedAt, Instant submittedAt, Instant expiresAt,
                                      long rowVersion, Instant createdAt, Instant updatedAt) {
    public static AssessmentAttemptResult from(AssessmentAttempt a) {
        return new AssessmentAttemptResult(a.getId(), a.getUserId(), a.getPackageVersionId(), a.getAttemptType(),
                a.getMode(), a.getChannel(), a.getStatus(), a.getStartedAt(), a.getSubmittedAt(), a.getExpiresAt(),
                a.getRowVersion(), a.getCreatedAt(), a.getUpdatedAt());
    }
}
