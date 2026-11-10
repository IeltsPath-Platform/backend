package com.ieltspath.user.application.exception;

public class GoogleAccountConflictException extends RuntimeException {
    public GoogleAccountConflictException() {
        super("A different Google account is already linked");
    }
}
