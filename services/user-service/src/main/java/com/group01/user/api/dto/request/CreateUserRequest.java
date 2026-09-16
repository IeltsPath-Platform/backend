package com.group01.user.api.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record CreateUserRequest(
        @Email(message = "Email không đúng định dạng")
        @NotBlank(message = "Email không được để trống")
        String email,

        @NotBlank(message = "Mật khẩu không được để trống")
        @Size(min = 6, max = 72, message = "Mật khẩu phải có từ 6 đến 72 ký tự")
        String password,

        @NotBlank(message = "Họ tên không được để trống")
        String fullName,

        String phoneNumber,
        Set<String> roles
) {
}
