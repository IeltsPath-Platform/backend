package com.group01.assessment.api.dto.request;
import jakarta.validation.constraints.DecimalMax; import jakarta.validation.constraints.DecimalMin;
public record CreateAssessmentResultRequest(@DecimalMin("0.0") @DecimalMax("9.0") Double overallBand) {}
