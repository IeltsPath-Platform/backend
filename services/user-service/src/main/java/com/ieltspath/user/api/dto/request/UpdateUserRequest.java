package com.ieltspath.user.api.dto.request;

import com.ieltspath.user.api.validation.PhoneInput;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;

public record UpdateUserRequest(@NotBlank(message = "Họ tên không được để trống")
        @Size(max = 150, message = "Họ tên không được vượt quá 150 ký tự")
        String fullName,
        @Size(max = 30, message = "Số điện thoại không được vượt quá 30 ký tự")
        @PhoneInput
        String phoneNumber) {
}
