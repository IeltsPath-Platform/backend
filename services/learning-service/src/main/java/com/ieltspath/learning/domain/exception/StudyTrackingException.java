package com.ieltspath.learning.domain.exception;

public class StudyTrackingException extends RuntimeException {
    private final String code;
    public StudyTrackingException(String code) { super(code); this.code = code; }
    public String getCode() { return code; }
}
