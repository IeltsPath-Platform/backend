package com.ieltspath.user.application.exception;

public class GoogleEmailNotSupportedException extends RuntimeException {
    public GoogleEmailNotSupportedException() {
        super("Địa chỉ email Google này chưa được hỗ trợ để đăng nhập.");
    }
}
