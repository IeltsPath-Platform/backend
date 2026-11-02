package com.ieltspath.user.application.exception;

public class LoginLimiterUnavailableException extends RuntimeException {
    public LoginLimiterUnavailableException(Throwable cause) {
        super("Login protection is temporarily unavailable", cause);
    }
}
