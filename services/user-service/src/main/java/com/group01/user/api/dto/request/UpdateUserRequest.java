package com.group01.user.api.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UpdateUserRequest(@NotBlank(message = "Họ tên không được để trống") String fullName, String phoneNumber) {
}
