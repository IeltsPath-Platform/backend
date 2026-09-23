package com.group01.assessment.api.dto.request;
import com.group01.assessment.domain.vo.*; import jakarta.validation.constraints.NotBlank; import jakarta.validation.constraints.NotNull; import java.util.UUID;
public record CreateGradingJobRequest(@NotNull UUID submissionId,@NotNull Skill skill,@NotNull GradingMode gradingMode,Integer pointCostSnapshot,@NotBlank String idempotencyKey) {}
