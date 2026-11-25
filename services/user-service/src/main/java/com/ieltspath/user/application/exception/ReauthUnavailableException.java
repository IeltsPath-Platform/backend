package com.ieltspath.user.application.exception;

public class ReauthUnavailableException extends RuntimeException {
    public ReauthUnavailableException() {
        super("Xác thực lại tạm thời không khả dụng. Vui lòng thử lại sau.");
    }
}
