package com.ieltspath.access.api.dto.request;

import com.ieltspath.access.domain.vo.LearningRewardReason;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreditPointsRequest(
        @NotNull UUID userId,
        @Min(1) @Max(100) int amount,
        @NotNull LearningRewardReason reason,
        @NotBlank @Size(max = 100) String referenceId,
        @NotBlank @Size(max = 120) String idempotencyKey
) {}
