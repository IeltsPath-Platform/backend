package com.ieltspath.user.application.exception;

public class AvatarUploadRateLimitException extends RuntimeException {
    public AvatarUploadRateLimitException() { super("Đã vượt quá giới hạn tải ảnh. Vui lòng thử lại sau."); }
}
