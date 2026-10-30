package com.ieltspath.content.api.dto.request;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record CreateCourseRequest(@NotBlank @Size(max = 100) String code,
                                  @NotBlank @Size(max = 255) String name,
                                  @NotNull @DecimalMin("0.0") @DecimalMax("9.0") BigDecimal bandLevel,
                                  @Size(max = 500) @Pattern(regexp = "(?i)^\\s*(https?://.*)?\\s*$") String coverImageUrl,
                                  @Size(max = 2000) String description) {
    public CreateCourseRequest(String code, String name, BigDecimal bandLevel) {
        this(code, name, bandLevel, null, null);
    }
}
