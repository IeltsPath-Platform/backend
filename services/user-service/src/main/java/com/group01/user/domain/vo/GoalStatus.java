package com.group01.user.domain.vo;

public enum GoalStatus {
    ACTIVE,
    ACHIEVED,
    ABANDONED,
    PAUSED;

    public static GoalStatus from(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Trạng thái mục tiêu không được để trống");
        }
        try {
            return GoalStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Trạng thái mục tiêu không hợp lệ: " + value);
        }
    }
}

