package com.ieltspath.library.api.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record WeeklyGoalRequest(@NotNull @Min(10) @Max(1000) Integer targetWords) {}
