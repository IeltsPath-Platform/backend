package com.group01.assessment.application.result;

import java.time.Instant;
import java.util.UUID;

public record AssessmentResultResult(UUID id, UUID attemptId, int resultVersion, String status,
                                     Double overallBand, Instant completedAt) {}
