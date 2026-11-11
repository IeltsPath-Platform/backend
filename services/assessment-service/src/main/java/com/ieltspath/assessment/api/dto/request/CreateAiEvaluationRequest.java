package com.ieltspath.assessment.api.dto.request;

import jakarta.validation.constraints.Pattern;
/** {@code templateVersion} (v1 or v2) runs a prompt template before it is activated; absent means v1. */
public record CreateAiEvaluationRequest(@Pattern(regexp = "TASK_[12]") String task,
        @Pattern(regexp = "v[12]") String templateVersion) {
    public CreateAiEvaluationRequest(String task) { this(task, null); }
}
