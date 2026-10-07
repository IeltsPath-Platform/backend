package com.ieltspath.access.domain.exception;

public class KeyExpiredException extends RuntimeException {

    public KeyExpiredException(String message) {
        super(message);
    }
}
