package com.group01.library.domain.exception;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException() {
        super("Không tìm thấy");
    }
}
