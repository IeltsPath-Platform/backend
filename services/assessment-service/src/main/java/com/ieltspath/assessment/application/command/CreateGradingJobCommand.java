package com.ieltspath.assessment.application.command;
import com.ieltspath.assessment.domain.vo.*; import java.util.UUID;
public record CreateGradingJobCommand(UUID userId, UUID submissionId, Skill skill, GradingMode gradingMode, Integer pointCostSnapshot, String idempotencyKey) {}
