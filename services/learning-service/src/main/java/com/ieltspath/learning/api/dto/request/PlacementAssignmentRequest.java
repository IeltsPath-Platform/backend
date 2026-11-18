package com.ieltspath.learning.api.dto.request;

import jakarta.validation.constraints.NotNull;

public record PlacementAssignmentRequest(@NotNull String pool) {}
