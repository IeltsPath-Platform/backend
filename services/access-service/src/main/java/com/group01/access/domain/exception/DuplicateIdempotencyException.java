package com.group01.access.domain.exception;

public class DuplicateIdempotencyException extends RuntimeException {

    public DuplicateIdempotencyException(String idempotencyKey) {
        super("Operation with idempotency key already exists: " + idempotencyKey);
    }
}
