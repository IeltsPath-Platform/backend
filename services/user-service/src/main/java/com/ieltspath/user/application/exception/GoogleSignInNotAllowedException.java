package com.ieltspath.user.application.exception;

public class GoogleSignInNotAllowedException extends RuntimeException {
    public GoogleSignInNotAllowedException() {
        super("Google sign-in is not allowed for this account");
    }
}
