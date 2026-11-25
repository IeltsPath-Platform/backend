package com.ieltspath.user.application.exception;

import java.time.Duration;

public class LoginRateLimitedException extends RuntimeException {
    private final Duration retryAfter;

    public LoginRateLimitedException(Duration retryAfter) {
        super("Bạn đã thử đăng nhập sai quá nhiều lần. Vui lòng thử lại sau.");
        this.retryAfter = retryAfter == null || retryAfter.isNegative() || retryAfter.isZero()
                ? Duration.ofSeconds(1) : retryAfter;
    }

    public Duration retryAfter() {
        return retryAfter;
    }
}
