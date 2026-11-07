package com.ieltspath.library.application.exception;

import org.springframework.http.HttpStatus;
import java.util.Map;

public class StudyRequestException extends RuntimeException {
    private final HttpStatus status;
    private final String code;
    private final Map<String, String> details;
    public StudyRequestException(HttpStatus status, String code, String message) { this(status, code, message, Map.of()); }
    public StudyRequestException(HttpStatus status, String code, String message, Map<String, String> details) {
        super(message); this.status = status; this.code = code; this.details = Map.copyOf(details);
    }
    public HttpStatus status() { return status; }
    public String code() { return code; }
    public Map<String, String> details() { return details; }
    public static StudyRequestException conflict(String code, String message) { return new StudyRequestException(HttpStatus.CONFLICT, code, message); }
    public static StudyRequestException missing(String code) { return new StudyRequestException(HttpStatus.NOT_FOUND, code, "Không tìm thấy nội dung học."); }
}
