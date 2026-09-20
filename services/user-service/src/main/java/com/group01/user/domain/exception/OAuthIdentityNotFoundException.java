package com.group01.user.domain.exception;

public class OAuthIdentityNotFoundException extends RuntimeException {
    public OAuthIdentityNotFoundException(String message) {
        super(message);
    }
}

