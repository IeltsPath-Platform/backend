package com.ieltspath.user.domain.exception;

public class PasswordNotConfiguredException extends RuntimeException {
    public PasswordNotConfiguredException() { super("Tài khoản này chưa thiết lập mật khẩu. Vui lòng đăng nhập bằng nhà cung cấp đã liên kết."); }
}
