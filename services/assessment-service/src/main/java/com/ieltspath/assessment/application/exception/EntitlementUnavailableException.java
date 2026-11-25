package com.ieltspath.assessment.application.exception;

public class EntitlementUnavailableException extends RuntimeException {
    public EntitlementUnavailableException(Throwable cause) {
        super("Chưa kiểm tra được gói Premium. Vui lòng thử lại sau.", cause);
    }
}
