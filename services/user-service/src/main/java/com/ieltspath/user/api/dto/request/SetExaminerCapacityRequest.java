package com.ieltspath.user.api.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** How many submissions an examiner may have open at once. */
public record SetExaminerCapacityRequest(@NotNull @Min(1) @Max(50) Integer dailyLimit) {}
