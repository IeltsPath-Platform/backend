package com.ieltspath.library.domain.exception;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException() {
        super("Không tìm thấy");
    }
}
