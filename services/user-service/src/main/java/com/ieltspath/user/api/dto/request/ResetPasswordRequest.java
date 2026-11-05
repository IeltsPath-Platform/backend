package com.ieltspath.user.api.dto.request;

import com.ieltspath.user.api.validation.PasswordLength;
import jakarta.validation.constraints.NotBlank;

public record ResetPasswordRequest(
        @NotBlank(message = "Mã token không được để trống")
        String token,

        @NotBlank(message = "Mật khẩu mới không được để trống")
        @PasswordLength
        String newPassword
) {
}

