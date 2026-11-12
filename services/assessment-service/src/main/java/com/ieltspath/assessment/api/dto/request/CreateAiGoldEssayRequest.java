package com.ieltspath.assessment.api.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import java.util.List;

/** {@code examinerErrors} is optional; each excerpt must be an exact part of {@code essay}. */
public record CreateAiGoldEssayRequest(@NotBlank @Pattern(regexp = "TASK_[12]") String task,
        @NotBlank String prompt, String chartFacts, @NotBlank String essay,
        @NotNull BigDecimal examinerBand, Object examinerCriteria, List<String> examinerErrorCodes,
        @Valid List<ExaminerErrorRequest> examinerErrors) {

    public CreateAiGoldEssayRequest(String task, String prompt, String chartFacts, String essay,
            BigDecimal examinerBand, Object examinerCriteria, List<String> examinerErrorCodes) {
        this(task, prompt, chartFacts, essay, examinerBand, examinerCriteria, examinerErrorCodes, List.of());
    }

    public record ExaminerErrorRequest(@NotBlank String excerpt, @NotBlank String errorCode,
            @NotBlank @Pattern(regexp = "LANGUAGE|DATA") String kind) {}
}
