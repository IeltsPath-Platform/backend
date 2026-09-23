package com.group01.access.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

public record DebitPointsRequest(
        @NotNull(message = "userId không được để trống")
        UUID userId,
        @Positive(message = "Số point trừ phải lớn hơn 0")
        long amount,
        @NotBlank(message = "referenceType không được để trống")
        String referenceType,
        @NotNull(message = "referenceId không được để trống")
        UUID referenceId,
        @NotBlank(message = "idempotencyKey không được để trống")
        String idempotencyKey,
        String description
        ) {

}
