package com.ieltspath.user.application.exception;

public class GoogleSignInNotAllowedException extends RuntimeException {
    public GoogleSignInNotAllowedException() {
        super("Loại tài khoản này không được đăng nhập bằng Google.");
    }
}
