package com.ieltspath.user.api.dto.request;

import com.ieltspath.user.api.validation.PasswordLength;
import com.ieltspath.user.api.validation.PhoneInput;
import com.ieltspath.user.domain.vo.FullName;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record CreateUserRequest(
        @Email(message = "Email không đúng định dạng")
        @NotBlank(message = "Email không được để trống")
        String email,

        @NotBlank(message = "Mật khẩu không được để trống")
        @PasswordLength
        String password,

        @NotBlank(message = "Họ tên không được để trống")
        @Size(max = FullName.MAX_LENGTH, message = FullName.TOO_LONG_MESSAGE)
        String fullName,

        @Size(max = 30, message = "Số điện thoại không được vượt quá 30 ký tự")
        @PhoneInput
        String phoneNumber,
        Set<String> roles
) {
}
