package com.group01.user.api.dto.request;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(String email, String username, @NotBlank(message = "Mật khẩu không được để trống") String password) {
    public String usernameOrEmail() {
        if (email != null && !email.isBlank()) {
            return email;
        }
        return username;
    }
}
