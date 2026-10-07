package com.ieltspath.user.domain.exception;

public class InvalidUserStatusException extends RuntimeException {
    public InvalidUserStatusException(String message) {
        super(message);
    }
}
