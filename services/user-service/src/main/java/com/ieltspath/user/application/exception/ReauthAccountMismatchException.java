package com.ieltspath.user.application.exception;

public class ReauthAccountMismatchException extends RuntimeException {
    public ReauthAccountMismatchException() {
        super("Tài khoản Google không khớp với tài khoản Google đã liên kết.");
    }
}
