package com.group01.learningsupport.api.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UpsertStreakRequest(
        @Min(0) int currentDays,
        @Min(0) int longestDays,
        LocalDate lastQualifiedDate,
        @NotBlank @Size(max = 100) String timezone
) {
}
