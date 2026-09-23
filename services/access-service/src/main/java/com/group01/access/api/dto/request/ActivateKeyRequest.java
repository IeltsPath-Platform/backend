package com.group01.access.api.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ActivateKeyRequest(
        @NotBlank(message = "Mã kích hoạt không được để trống")
        String rawKey,
        @NotBlank(message = "Idempotency key không được để trống")
        String idempotencyKey
        ) {

}
