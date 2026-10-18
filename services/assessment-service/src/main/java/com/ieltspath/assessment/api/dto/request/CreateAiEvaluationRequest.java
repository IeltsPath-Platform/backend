package com.ieltspath.assessment.api.dto.request;

import jakarta.validation.constraints.Pattern;
public record CreateAiEvaluationRequest(@Pattern(regexp = "TASK_[12]") String task) {}
