package com.ieltspath.assessment.application.result;

import com.ieltspath.assessment.domain.vo.GradingJobStatus;
import com.ieltspath.assessment.domain.vo.GradingMode;
import com.ieltspath.assessment.domain.vo.Skill;

import java.time.Instant;
import java.util.UUID;

public record GradingJobResult(UUID id, UUID submissionId, UUID userId, Skill skill, GradingMode gradingMode,
                               GradingJobStatus status, Integer pointCostSnapshot, String idempotencyKey,
                               Instant createdAt, Instant completedAt) {}
