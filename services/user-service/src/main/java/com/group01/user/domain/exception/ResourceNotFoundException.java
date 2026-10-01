package com.group01.user.domain.exception;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException() {
        super("Không tìm thấy");
    }
}
