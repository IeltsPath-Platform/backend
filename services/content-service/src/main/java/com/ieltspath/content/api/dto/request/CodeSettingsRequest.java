package com.ieltspath.content.api.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class CodeSettingsRequest {
    private CodeSettingsRequest() {}

    public record Update(@NotNull Boolean applied, @NotBlank @Size(max = 255) String pattern,
                         @NotNull @Min(0) Long startNumber, @NotNull Boolean editable) {}
}
