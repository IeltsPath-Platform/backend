package com.group01.user.domain.exception;

public class InvalidActionTokenException extends RuntimeException {
    public InvalidActionTokenException(String message) {
        super(message);
    }
}

