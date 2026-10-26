package com.ieltspath.assessment.api.dto.request;

import com.ieltspath.assessment.application.command.CompleteHumanReviewCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record CompleteHumanReviewRequest(
        @NotNull @DecimalMin("0.0") @DecimalMax("9.0") BigDecimal band,
        @NotEmpty List<@Valid Criterion> criteria,
        @NotNull List<@NotBlank String> errorCodes,
        @NotBlank String feedback,
        @Size(max = 200) List<@Valid @NotNull Annotation> annotations
) {
    public record Criterion(@NotBlank String criterion,
                            @NotNull @DecimalMin("0.0") @DecimalMax("9.0") BigDecimal band,
                            @NotBlank String comment) {}

    /** An error the examiner marked in the essay; {@code errorCode} must be an error-type code. */
    public record Annotation(@NotBlank @Size(max = 500) String excerpt,
                             @NotNull @PositiveOrZero Integer start,
                             @NotNull @Positive Integer end,
                             @NotBlank @Size(max = 60) String errorCode,
                             @NotBlank @Size(max = 1000) String suggestion) {}

    public CompleteHumanReviewCommand toCommand() {
        return new CompleteHumanReviewCommand(band,
                criteria.stream().map(c -> new CompleteHumanReviewCommand.Criterion(c.criterion(), c.band(), c.comment()))
                        .toList(),
                errorCodes,
                annotations == null ? List.of() : annotations.stream()
                        .map(a -> new CompleteHumanReviewCommand.Annotation(a.excerpt(), a.start(), a.end(),
                                a.errorCode(), a.suggestion()))
                        .toList(),
                feedback);
    }
}
