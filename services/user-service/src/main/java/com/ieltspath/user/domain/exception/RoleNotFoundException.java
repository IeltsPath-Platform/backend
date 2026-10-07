package com.ieltspath.user.domain.exception;

public class RoleNotFoundException extends RuntimeException {
    public RoleNotFoundException(String message) {
        super(message);
    }
}
