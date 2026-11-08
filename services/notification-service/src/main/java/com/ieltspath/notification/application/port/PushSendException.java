package com.ieltspath.notification.application.port;

public class PushSendException extends RuntimeException {
    public enum Category { RETRYABLE, UNREGISTERED, PERMANENT, CONFIGURATION }

    private final Category category;
    private final String code;
    private final long retryAfterSeconds;

    public PushSendException(Category category, String code) {
        this(category, code, 0);
    }

    public PushSendException(Category category, String code, long retryAfterSeconds) {
        super("FCM delivery failed: " + code);
        this.category = category;
        this.code = code;
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public Category category() { return category; }
    public String code() { return code; }
    public long retryAfterSeconds() { return retryAfterSeconds; }
}
