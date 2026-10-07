package com.group01.content.api.dto.request;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record CreateCourseRequest(@NotBlank @Size(max = 100) String code,
                                  @NotBlank @Size(max = 255) String name,
                                  @NotNull @DecimalMin("0.0") @DecimalMax("9.0") BigDecimal bandLevel) {}
