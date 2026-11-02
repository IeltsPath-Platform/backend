package com.ieltspath.access.application.exception;

public class InvalidIdempotencyKeyException extends RuntimeException {
    public InvalidIdempotencyKeyException() {
        super("Idempotency-Key is required and must contain at most 100 characters");
    }
}
