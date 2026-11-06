package com.ieltspath.user.domain.vo;

public enum RoleName {
    ADMIN,
    CUSTOMER,
    CONTENT_AUTHOR,
    EXAMINER,
    SALES_STAFF,
    STAFF_MANAGER;

    public static RoleName from(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Tên vai trò không được để trống");
        }
        String name = value.trim();
        try {
            return RoleName.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException unknown) {
            throw new IllegalArgumentException("Vai trò không tồn tại: " + name);
        }
    }
}
