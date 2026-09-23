package com.group01.access.api.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public record GenerateKeysRequest(
        @NotNull(message = "productId không được để trống")
        UUID productId,
        @Min(value = 1, message = "Số lượng key tối thiểu là 1")
        @Max(value = 1000, message = "Số lượng key tối đa là 1000")
        int count,
        Instant expiresAt
        ) {

}
