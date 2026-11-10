package com.ieltspath.notification.application.port;

public class PushTransientException extends org.springframework.web.client.ResourceAccessException {
    private final String code;
    private final long retryAfterSeconds;

    public PushTransientException(String code, long retryAfterSeconds) {
        super("FCM temporary delivery failure: " + code);
        this.code = code;
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public String code() { return code; }
    public long retryAfterSeconds() { return retryAfterSeconds; }
}
