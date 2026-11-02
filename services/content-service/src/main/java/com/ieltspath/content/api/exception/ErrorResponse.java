package com.ieltspath.content.api.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import java.util.Map;

public record ErrorResponse(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        String path,
        Map<String, Object> details,
        @JsonInclude(JsonInclude.Include.NON_NULL) String code
) {
    public ErrorResponse(LocalDateTime timestamp, int status, String error, String message, String path,
                         Map<String, Object> details) {
        this(timestamp, status, error, message, path, details, null);
    }
}

