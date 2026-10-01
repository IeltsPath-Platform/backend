package com.group01.learningsupport.api.dto.request;

import com.group01.learningsupport.domain.vo.VideoProgressStatus;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record UpsertVideoProgressRequest(
        @NotNull UUID videoId,
        @Min(0) int lastPositionMs,
        @Min(0) int watchedDurationSeconds,
        @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal progressPercent,
        @NotNull VideoProgressStatus status,
        Instant startedAt,
        Instant lastWatchedAt,
        Instant completedAt
) {
}
