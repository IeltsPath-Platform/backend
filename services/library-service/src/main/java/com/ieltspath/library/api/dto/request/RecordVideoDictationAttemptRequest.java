package com.ieltspath.library.api.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

/** The frontend compares the typed text with the segment transcript and sends its verdict. */
public record RecordVideoDictationAttemptRequest(
        @NotNull UUID segmentId,
        @NotBlank @Size(max = 2000) String typedText,
        @NotNull Boolean correct,
        @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal accuracyPercent
) {
}
