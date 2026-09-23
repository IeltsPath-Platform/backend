package com.group01.access.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AdjustPointsRequest(
        @NotNull(message = "userId không được để trống")
        UUID userId,
        long delta,
        @NotBlank(message = "Lý do điều chỉnh không được để trống")
        String reason,
        @NotBlank(message = "idempotencyKey không được để trống")
        String idempotencyKey
        ) {

}
