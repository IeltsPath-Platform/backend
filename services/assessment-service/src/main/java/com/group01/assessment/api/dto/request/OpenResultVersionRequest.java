package com.group01.assessment.api.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

/** Opens the next result version of an attempt for grading. Same band bounds as {@link CreateAssessmentResultRequest}. */
public record OpenResultVersionRequest(@DecimalMin("0.0") @DecimalMax("9.0") Double overallBand) {
}
