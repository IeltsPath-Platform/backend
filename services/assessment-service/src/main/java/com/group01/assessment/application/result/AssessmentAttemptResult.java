package com.group01.assessment.application.result;

import com.group01.assessment.domain.vo.*;

import java.time.Instant;
import java.util.UUID;

public record AssessmentAttemptResult(UUID id, UUID userId, UUID packageVersionId, AttemptType attemptType,
                                      AttemptMode mode, AttemptChannel channel, AttemptStatus status,
                                      Instant startedAt, Instant submittedAt, Instant expiresAt,
                                      long rowVersion, Instant createdAt, Instant updatedAt) {}
