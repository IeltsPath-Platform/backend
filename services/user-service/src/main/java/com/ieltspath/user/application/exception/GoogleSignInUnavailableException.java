package com.ieltspath.user.application.exception;

public class GoogleSignInUnavailableException extends RuntimeException {
    public GoogleSignInUnavailableException() {
        super("Đăng nhập Google tạm thời không khả dụng. Vui lòng thử lại sau.");
    }

    public GoogleSignInUnavailableException(Throwable cause) {
        super("Đăng nhập Google tạm thời không khả dụng. Vui lòng thử lại sau.", cause);
    }
}
