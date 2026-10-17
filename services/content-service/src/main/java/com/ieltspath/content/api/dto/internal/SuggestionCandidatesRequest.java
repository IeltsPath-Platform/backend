package com.ieltspath.content.api.dto.internal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record SuggestionCandidatesRequest(
        @NotEmpty @Size(max = 50) List<@NotNull UUID> knowledgePointIds,
        @NotNull @DecimalMin("0.0") @DecimalMax("9.0") BigDecimal maxBand
) {}
