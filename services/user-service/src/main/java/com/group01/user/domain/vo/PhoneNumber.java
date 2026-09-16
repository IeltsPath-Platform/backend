package com.group01.user.domain.vo;

public record PhoneNumber(String value) {
    public PhoneNumber {
        if (value != null) {
            value = value.trim();
            if (value.isBlank()) {
                value = null;
            }
        }
    }
}
