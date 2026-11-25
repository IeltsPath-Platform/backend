package com.ieltspath.user.application.exception;

public class PasswordAlreadySetException extends RuntimeException {
    public PasswordAlreadySetException() {
        super("Tài khoản đã có mật khẩu. Hãy dùng chức năng đổi mật khẩu.");
    }
}
