package com.ieltspath.learning.domain.exception;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException() {
        super("Không tìm thấy");
    }
}
