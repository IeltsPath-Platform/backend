package com.ieltspath.access.application.service;

import com.ieltspath.access.application.exception.InvalidIdempotencyKeyException;

/** The checks the issue endpoints share for the client's Idempotency-Key header. */
public final class KeyIdempotency {
    private static final int MAX_LENGTH = 100;

    private KeyIdempotency() {
    }

    public static void require(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank() || idempotencyKey.length() > MAX_LENGTH) {
            throw new InvalidIdempotencyKeyException();
        }
    }
}
