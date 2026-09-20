package com.group01.user.domain.vo;

public enum OAuthProvider {
    GOOGLE,
    FACEBOOK,
    APPLE;

    public static OAuthProvider from(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("OAuth provider không được để trống");
        }
        try {
            return OAuthProvider.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("OAuth provider không hợp lệ: " + value);
        }
    }
}

