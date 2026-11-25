package com.ieltspath.user.application.exception;

import com.ieltspath.user.domain.exception.AuthenticationFailedException;

public class GoogleIdTokenInvalidException extends AuthenticationFailedException {
    public GoogleIdTokenInvalidException() {
        super("Phiên đăng nhập Google không hợp lệ hoặc đã hết hạn. Vui lòng thử lại.");
    }
}
