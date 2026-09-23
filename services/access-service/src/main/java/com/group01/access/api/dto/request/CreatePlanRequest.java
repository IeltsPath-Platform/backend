package com.group01.access.api.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CreatePlanRequest(
        @NotBlank(message = "Mã gói không được để trống")
        String code,
        @NotBlank(message = "Tên gói không được để trống")
        String name
        ) {

}
