package com.ieltspath.assessment.api.dto.request;

import com.ieltspath.assessment.application.command.CompleteHumanReviewCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record CompleteHumanReviewRequest(
        @NotNull @DecimalMin("0.0") @DecimalMax("9.0") BigDecimal band,
        @NotEmpty List<@Valid Criterion> criteria,
        @NotNull List<@NotBlank String> errorCodes,
        @NotBlank String feedback
) {
    public record Criterion(@NotBlank String criterion,
                            @NotNull @DecimalMin("0.0") @DecimalMax("9.0") BigDecimal band,
                            @NotBlank String comment) {}

    public CompleteHumanReviewCommand toCommand() {
        return new CompleteHumanReviewCommand(band,
                criteria.stream().map(c -> new CompleteHumanReviewCommand.Criterion(c.criterion(), c.band(), c.comment()))
                        .toList(),
                errorCodes, feedback);
    }
}
