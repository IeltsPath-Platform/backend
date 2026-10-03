package com.group01.learning.domain.exception;

public class PracticeAttemptAlreadySubmittedException extends RuntimeException {
    public PracticeAttemptAlreadySubmittedException() {
        super("Practice attempt was already submitted with a different requestId");
    }
}
