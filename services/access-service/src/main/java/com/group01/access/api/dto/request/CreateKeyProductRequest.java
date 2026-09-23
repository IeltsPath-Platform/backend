package com.group01.access.api.dto.request;

import com.group01.access.domain.vo.KeyType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateKeyProductRequest(
        @NotBlank(message = "Mã sản phẩm không được để trống")
        String code,
        @NotBlank(message = "Tên sản phẩm không được để trống")
        String name,
        @NotNull(message = "Loại key không được để trống")
        KeyType keyType,
        Integer pointsAmount,
        UUID planId,
        Integer premiumDays,
        Integer humanGradingCredits
        ) {

}
