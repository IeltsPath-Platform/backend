package com.group01.learning.application.exception;

public class LearningRequestException extends RuntimeException {
    private final int status;
    private final String code;

    public LearningRequestException(int status, String code, String detail) {
        super(detail);
        this.status = status;
        this.code = code;
    }

    public int getStatus() { return status; }
    public String getCode() { return code; }
}
