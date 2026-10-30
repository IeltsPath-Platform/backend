package com.ieltspath.content.api.dto.request;

import com.ieltspath.content.domain.vo.ContentStatus;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record UpdateCourseRequest(@NotBlank @Size(max = 255) String name,
                                  @NotNull @DecimalMin("0.0") @DecimalMax("9.0") BigDecimal bandLevel,
                                  ContentStatus status,
                                  @Size(max = 500) @Pattern(regexp = "(?i)^\\s*(https?://.*)?\\s*$") String coverImageUrl,
                                  @Size(max = 2000) String description) {
    public UpdateCourseRequest(String name, BigDecimal bandLevel, ContentStatus status) {
        this(name, bandLevel, status, null, null);
    }
}
