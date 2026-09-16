package com.group01.user.domain.vo;

import java.util.regex.Pattern;

public record Email(String value) {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    public Email {
        if (value == null || value.isBlank() || !EMAIL_PATTERN.matcher(value.trim()).matches()) {
            throw new IllegalArgumentException("Email không hợp lệ");
        }
        value = value.trim().toLowerCase();
    }
}
