package com.ieltspath.assessment.application.command;

import java.util.UUID;

public record SubmitAssessmentAttemptCommand(UUID userId, UUID attemptId) {}
