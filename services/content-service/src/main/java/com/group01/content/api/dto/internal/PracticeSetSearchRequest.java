package com.group01.content.api.dto.internal;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

/** Omitted fields take defaults: no exclusions, at least 3 questions, one result. */
public record PracticeSetSearchRequest(
        @NotNull(message = "knowledgePointId is required")
        UUID knowledgePointId,
        List<UUID> excludePackageIds,
        @Min(value = 1, message = "minQuestions must be at least 1")
        Integer minQuestions,
        @Min(value = 1, message = "limit must be between 1 and 10")
        @Max(value = 10, message = "limit must be between 1 and 10")
        Integer limit
) {}
