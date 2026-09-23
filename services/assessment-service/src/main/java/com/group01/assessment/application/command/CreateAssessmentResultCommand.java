package com.group01.assessment.application.command;

import java.util.UUID;

public record CreateAssessmentResultCommand(UUID userId, UUID attemptId, Double overallBand) {}
