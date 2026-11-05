package com.ieltspath.user.application.exception;

import java.time.Duration;

public class LoginRateLimitedException extends RuntimeException {
    private final Duration retryAfter;

    public LoginRateLimitedException(Duration retryAfter) {
        super("Too many login attempts. Try again later.");
        this.retryAfter = retryAfter == null || retryAfter.isNegative() || retryAfter.isZero()
                ? Duration.ofSeconds(1) : retryAfter;
    }

    public Duration retryAfter() {
        return retryAfter;
    }
}
