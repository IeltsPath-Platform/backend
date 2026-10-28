package com.ieltspath.user.application.command;

/** Where a sign-in came from; values are cut to the column sizes of the login history. */
public record LoginContext(String ipAddress, String userAgent) {
    public static final LoginContext UNKNOWN = new LoginContext(null, null);

    public LoginContext {
        ipAddress = cut(ipAddress, 45);
        userAgent = cut(userAgent, 255);
    }

    private static String cut(String value, int max) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() <= max ? trimmed : trimmed.substring(0, max);
    }
}
