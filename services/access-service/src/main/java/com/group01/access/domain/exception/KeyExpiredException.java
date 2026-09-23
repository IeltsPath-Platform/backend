package com.group01.access.domain.exception;

public class KeyExpiredException extends RuntimeException {

    public KeyExpiredException(String message) {
        super(message);
    }
}
