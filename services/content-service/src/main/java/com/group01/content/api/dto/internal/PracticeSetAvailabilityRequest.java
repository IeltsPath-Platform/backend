package com.group01.content.api.dto.internal;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

/** Omitted fields take defaults: no exclusions, at least 3 questions. */
public record PracticeSetAvailabilityRequest(
        @NotEmpty(message = "knowledgePointIds is required")
        @Size(max = 50, message = "knowledgePointIds must hold at most 50 ids")
        List<@NotNull UUID> knowledgePointIds,
        @Size(max = 1000, message = "excludePackageIds must hold at most 1000 ids")
        List<@NotNull UUID> excludePackageIds,
        @Min(value = 1, message = "minQuestions must be at least 1")
        Integer minQuestions
) {}
