package com.ieltspath.assessment.api.dto.request;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record SubmitExaminerQaReviewRequest(
        @NotNull UUID gradingJobId,
        @NotNull @DecimalMin("0") @DecimalMax("9") BigDecimal reviewerBand,
        @NotNull JsonNode reviewerCriteria,
        @Size(max = 4000) String note
) {}
