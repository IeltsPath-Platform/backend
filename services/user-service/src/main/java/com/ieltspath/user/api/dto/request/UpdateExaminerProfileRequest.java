package com.ieltspath.user.api.dto.request;

import com.ieltspath.user.application.command.UpdateExaminerProfileCommand;
import com.ieltspath.user.application.result.ExaminerResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record UpdateExaminerProfileRequest(
        @DecimalMin("0.0") @DecimalMax("9.0") BigDecimal ieltsBand,
        @Size(max = 10) List<@Valid @NotNull Certificate> certificates,
        @NotNull Boolean acceptingSubmissions) {

    public record Certificate(@NotBlank @Size(max = 150) String name, @Size(max = 150) String issuer,
                              @Min(1950) @Max(2100) Integer year, @Size(max = 20) String score) {}

    public UpdateExaminerProfileCommand toCommand() {
        return new UpdateExaminerProfileCommand(ieltsBand, certificates == null ? List.of() : certificates.stream()
                .map(c -> new ExaminerResult.Certificate(c.name(), c.issuer(), c.year(), c.score())).toList(),
                acceptingSubmissions);
    }
}
