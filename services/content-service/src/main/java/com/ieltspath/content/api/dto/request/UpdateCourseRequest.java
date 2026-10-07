package com.ieltspath.content.api.dto.request;

import com.ieltspath.content.domain.vo.ContentStatus;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record UpdateCourseRequest(@NotBlank @Size(max = 255) String name,
                                  @NotNull @DecimalMin("0.0") @DecimalMax("9.0") BigDecimal bandLevel,
                                  ContentStatus status) {}
