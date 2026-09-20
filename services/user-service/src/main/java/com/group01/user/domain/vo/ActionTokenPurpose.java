package com.group01.user.domain.vo;

public enum ActionTokenPurpose {
    PASSWORD_RESET,
    EMAIL_VERIFICATION,
    ACCOUNT_ACTIVATION;

    public static ActionTokenPurpose from(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Mục đích token không được để trống");
        }
        try {
            return ActionTokenPurpose.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Mục đích token không hợp lệ: " + value);
        }
    }
}

