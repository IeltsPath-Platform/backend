package com.ieltspath.assessment.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import java.util.List;

public record CreateAiGoldEssayRequest(@NotBlank @Pattern(regexp = "TASK_[12]") String task,
        @NotBlank String prompt, String chartFacts, @NotBlank String essay,
        @NotNull BigDecimal examinerBand, Object examinerCriteria, List<String> examinerErrorCodes) {}
