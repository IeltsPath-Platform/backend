package com.ieltspath.user.application.exception;

public class GoogleSignInUnavailableException extends RuntimeException {
    public GoogleSignInUnavailableException() {
        super("Google sign-in is temporarily unavailable");
    }

    public GoogleSignInUnavailableException(Throwable cause) {
        super("Google sign-in is temporarily unavailable", cause);
    }
}
