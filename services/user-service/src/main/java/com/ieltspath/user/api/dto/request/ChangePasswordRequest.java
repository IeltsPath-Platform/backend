package com.ieltspath.user.api.dto.request;

import com.ieltspath.user.api.validation.PasswordLength;
import jakarta.validation.constraints.NotBlank;

public record ChangePasswordRequest(
        @NotBlank(message = "Mật khẩu hiện tại không được để trống") String currentPassword,
        @NotBlank(message = "Mật khẩu mới không được để trống")
        @PasswordLength String newPassword
) { }
