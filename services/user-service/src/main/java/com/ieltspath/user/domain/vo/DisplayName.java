package com.ieltspath.user.domain.vo;

/**
 * The name a learner shows to others: required, trimmed, and at most {@value #MAX_LENGTH} characters, the size of the
 * {@code learner_profiles.display_name} column. A new profile is named after the account's full name, so this limit
 * must never be lower than {@link FullName#MAX_LENGTH}; otherwise a learner could not save the profile as it stands.
 */
public record DisplayName(String value) {

    public static final int MAX_LENGTH = 150;
    public static final String BLANK_MESSAGE = "Tên hiển thị không được để trống";
    public static final String TOO_LONG_MESSAGE = "Tên hiển thị không được vượt quá " + MAX_LENGTH + " ký tự";

    public DisplayName {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(BLANK_MESSAGE);
        }
        value = value.trim();
        if (value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException(TOO_LONG_MESSAGE);
        }
    }
}
