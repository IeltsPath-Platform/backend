package com.ieltspath.user.domain.vo;

import java.util.regex.Pattern;

public record PhoneNumber(String value) {

    private static final Pattern SEPARATORS = Pattern.compile("[\\s.\\-()]");
    private static final Pattern NUMBER = Pattern.compile("^\\+?[0-9]{8,15}$");

    public PhoneNumber {
        if (value != null) {
            value = value.trim();
            if (value.isBlank()) {
                value = null;
            }
        }
    }

    /**
     * Reads a phone number typed by a user: spaces, dots, dashes and brackets are removed and what is left must be
     * 8 to 15 digits with an optional leading +. Blank input means no phone number. The constructor stays lenient
     * because rows saved before this rule must still load.
     */
    public static PhoneNumber parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return new PhoneNumber(null);
        }
        String cleaned = SEPARATORS.matcher(raw).replaceAll("");
        if (!NUMBER.matcher(cleaned).matches()) {
            throw new IllegalArgumentException("Số điện thoại không hợp lệ");
        }
        return new PhoneNumber(cleaned);
    }

    public static boolean isValidInput(String raw) {
        try {
            parse(raw);
            return true;
        } catch (IllegalArgumentException invalid) {
            return false;
        }
    }
}
