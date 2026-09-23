package com.group01.assessment.application.command;
import com.group01.assessment.domain.vo.*; import java.util.UUID;
public record CreateGradingJobCommand(UUID userId, UUID submissionId, Skill skill, GradingMode gradingMode, Integer pointCostSnapshot, String idempotencyKey) {}
