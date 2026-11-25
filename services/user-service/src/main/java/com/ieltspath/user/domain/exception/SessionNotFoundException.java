package com.ieltspath.user.domain.exception;

public class SessionNotFoundException extends RuntimeException {
    public SessionNotFoundException() {
        super("Không tìm thấy phiên đăng nhập.");
    }
}
