package com.group01.assessment.domain.entity;

import java.time.Instant;
import java.util.UUID;

public record AssessmentResult(UUID id, UUID attemptId, int resultVersion, String status,
                               Double overallBand, Instant completedAt) {}
