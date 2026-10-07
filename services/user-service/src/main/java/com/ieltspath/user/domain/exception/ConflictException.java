package com.ieltspath.user.domain.exception;

public class ConflictException extends RuntimeException {
    public ConflictException() {
        super("Dữ liệu đã tồn tại");
    }
}
