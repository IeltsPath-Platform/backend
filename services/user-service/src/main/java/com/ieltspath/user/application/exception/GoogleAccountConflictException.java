package com.ieltspath.user.application.exception;

public class GoogleAccountConflictException extends RuntimeException {
    public GoogleAccountConflictException() {
        super("Tài khoản Google này đã được liên kết với một tài khoản khác.");
    }
}
