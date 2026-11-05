package com.ieltspath.user.domain.vo;

import java.nio.charset.StandardCharsets;

/** Length rule for new passwords. BCrypt reads at most 72 bytes, so the limit counts UTF-8 bytes, not characters. */
public final class PasswordPolicy {

    public static final int MIN_LENGTH = 6;
    public static final int MAX_BYTES = 72;
    public static final String MESSAGE =
            "Mật khẩu phải có từ 6 đến 72 byte (chữ có dấu tính nhiều byte hơn)";

    private PasswordPolicy() {
    }

    public static boolean isWithinLimits(String password) {
        return password != null && password.length() >= MIN_LENGTH && byteLength(password) <= MAX_BYTES;
    }

    public static boolean exceedsMaxBytes(String password) {
        return password != null && byteLength(password) > MAX_BYTES;
    }

    private static int byteLength(String password) {
        return password.getBytes(StandardCharsets.UTF_8).length;
    }
}
