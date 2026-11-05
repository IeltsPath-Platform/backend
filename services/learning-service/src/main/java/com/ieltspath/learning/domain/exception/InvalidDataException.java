package com.ieltspath.learning.domain.exception;

public class InvalidDataException extends RuntimeException {
    public InvalidDataException() {
        super("Dữ liệu không hợp lệ");
    }

    public InvalidDataException(String message) {
        super(message);
    }
}
