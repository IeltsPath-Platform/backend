package com.ieltspath.access.application.exception;

public class InvalidIdempotencyKeyException extends RuntimeException {
    public InvalidIdempotencyKeyException() {
        super("Thiếu mã chống gửi trùng (Idempotency-Key) hoặc mã dài quá 100 ký tự");
    }
}
