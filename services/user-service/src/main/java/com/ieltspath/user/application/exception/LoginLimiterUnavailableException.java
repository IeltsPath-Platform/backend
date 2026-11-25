package com.ieltspath.user.application.exception;

public class LoginLimiterUnavailableException extends RuntimeException {
    public LoginLimiterUnavailableException(Throwable cause) {
        super("Hệ thống tạm thời không thể xử lý đăng nhập. Vui lòng thử lại sau.", cause);
    }
}
