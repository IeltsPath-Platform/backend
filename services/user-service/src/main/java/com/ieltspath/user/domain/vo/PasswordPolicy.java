package com.ieltspath.user.domain.vo;

import java.nio.charset.StandardCharsets;

/** Length rule for new passwords. BCrypt reads at most 72 bytes, so the limit counts UTF-8 bytes, not characters. */
public final class PasswordPolicy {

    public static final int MIN_LENGTH = 8;
    public static final int MAX_BYTES = 72;
    public static final String MESSAGE =
            "Mật khẩu phải có từ 8 đến 72 ký tự (chữ có dấu tính nhiều byte hơn)";

    public static String message(int minLength) {
        return "Mật khẩu phải có từ " + minLength + " đến 72 ký tự (chữ có dấu tính nhiều byte hơn)";
    }

    private PasswordPolicy() {
    }

    public static boolean isWithinLimits(String password) {
        return isWithinLimits(password, MIN_LENGTH);
    }

    public static boolean isWithinLimits(String password, int minLength) {
        return password != null && password.length() >= minLength && byteLength(password) <= MAX_BYTES;
    }

    public static boolean isTooSimple(String password) {
        if (password == null || password.length() < 6) return false;
        int[] codePoints = password.toLowerCase(java.util.Locale.ROOT).codePoints().toArray();
        boolean repeated = true;
        boolean ascending = true;
        boolean descending = true;
        for (int i = 1; i < codePoints.length; i++) {
            repeated &= codePoints[i] == codePoints[0];
            ascending &= codePoints[i] == codePoints[i - 1] + 1;
            descending &= codePoints[i] == codePoints[i - 1] - 1;
        }
        return repeated || ascending || descending;
    }

    public static boolean exceedsMaxBytes(String password) {
        return password != null && byteLength(password) > MAX_BYTES;
    }

    private static int byteLength(String password) {
        return password.getBytes(StandardCharsets.UTF_8).length;
    }
}
