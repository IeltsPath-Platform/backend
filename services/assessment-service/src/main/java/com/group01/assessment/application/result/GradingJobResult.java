package com.group01.assessment.application.result;

import com.group01.assessment.domain.vo.GradingJobStatus;
import com.group01.assessment.domain.vo.GradingMode;
import com.group01.assessment.domain.vo.Skill;

import java.time.Instant;
import java.util.UUID;

public record GradingJobResult(UUID id, UUID submissionId, UUID userId, Skill skill, GradingMode gradingMode,
                               GradingJobStatus status, Integer pointCostSnapshot, String idempotencyKey,
                               Instant createdAt, Instant completedAt) {}
