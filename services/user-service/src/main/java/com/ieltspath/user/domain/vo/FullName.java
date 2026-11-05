package com.ieltspath.user.domain.vo;

/**
 * The full name of an account: required, and at most {@value #MAX_LENGTH} characters, the size of the
 * {@code users.full_name} column. The name is trimmed. The default display name of a learner is this name, so the
 * display name limit must never be lower than {@link #MAX_LENGTH}.
 */
public record FullName(String value) {

    public static final int MAX_LENGTH = 150;
    public static final String BLANK_MESSAGE = "Họ tên không được để trống";
    public static final String TOO_LONG_MESSAGE = "Họ tên không được vượt quá " + MAX_LENGTH + " ký tự";

    public FullName {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(BLANK_MESSAGE);
        }
        value = value.trim();
        if (value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException(TOO_LONG_MESSAGE);
        }
    }
}
