package com.group01.access.api.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record GrantSubscriptionRequest(
        @NotNull(message = "userId không được để trống")
        UUID userId,
        @NotNull(message = "planId không được để trống")
        UUID planId,
        @Min(value = 1, message = "Thời hạn tối thiểu là 1 ngày")
        int durationDays,
        @Min(value = 0, message = "Số lượt chấm human không được âm")
        int humanGradingCredits
        ) {

}
