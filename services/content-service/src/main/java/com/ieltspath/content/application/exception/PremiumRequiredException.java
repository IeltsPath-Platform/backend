package com.ieltspath.content.application.exception;

public class PremiumRequiredException extends RuntimeException {
    public PremiumRequiredException() {
        super("Đề này dành cho tài khoản Premium. Nâng cấp để luyện đề này.");
    }
}
