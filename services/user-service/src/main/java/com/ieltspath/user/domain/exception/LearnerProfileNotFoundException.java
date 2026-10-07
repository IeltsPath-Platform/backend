package com.ieltspath.user.domain.exception;

public class LearnerProfileNotFoundException extends RuntimeException {
    public LearnerProfileNotFoundException(String message) {
        super(message);
    }
}

