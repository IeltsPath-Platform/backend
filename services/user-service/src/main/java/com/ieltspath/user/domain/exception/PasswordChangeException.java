package com.ieltspath.user.domain.exception;

public class PasswordChangeException extends RuntimeException {
    private final String code;
    private final boolean rateLimited;
    public PasswordChangeException(String code, String message) { this(code, message, false); }
    public PasswordChangeException(String code, String message, boolean rateLimited) { super(message); this.code = code; this.rateLimited = rateLimited; }
    public String getCode() { return code; }
    public boolean isRateLimited() { return rateLimited; }
}
