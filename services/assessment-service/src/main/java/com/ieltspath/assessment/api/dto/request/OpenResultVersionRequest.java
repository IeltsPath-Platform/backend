package com.ieltspath.assessment.api.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

/** Opens the next result version of an attempt for grading. Band, when given, is 0.0–9.0. */
public record OpenResultVersionRequest(@DecimalMin("0.0") @DecimalMax("9.0") Double overallBand) {
}
