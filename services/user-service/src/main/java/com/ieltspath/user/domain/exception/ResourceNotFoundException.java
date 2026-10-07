package com.ieltspath.user.domain.exception;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException() {
        super("Không tìm thấy");
    }
}
