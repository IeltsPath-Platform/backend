package com.group01.user.domain.vo;

public enum RoleName {
    ADMIN,
    LEARNER;

    public static RoleName from(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Tên vai trò không được để trống");
        }
        return RoleName.valueOf(value.trim().toUpperCase());
    }
}
