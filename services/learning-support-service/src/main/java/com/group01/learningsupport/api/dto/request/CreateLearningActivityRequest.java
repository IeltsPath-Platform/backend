package com.group01.learningsupport.api.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public record CreateLearningActivityRequest(
        @NotBlank @Size(max = 50) String activityType,
        @NotBlank @Size(max = 50) String sourceType,
        @NotNull UUID sourceId,
        @NotNull Instant occurredAt,
        @Min(0) Integer durationSeconds
) {
}
