package com.ieltspath.content.application.exception;

public class VocabularyServiceUnavailableException extends RuntimeException {
    public VocabularyServiceUnavailableException(Throwable cause) {
        super("Dịch vụ từ vựng tạm thời chưa khả dụng, vui lòng thử lại sau", cause);
    }
}
