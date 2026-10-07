package com.ieltspath.user.domain.exception;

public class OAuthIdentityNotFoundException extends RuntimeException {
    public OAuthIdentityNotFoundException(String message) {
        super(message);
    }
}

