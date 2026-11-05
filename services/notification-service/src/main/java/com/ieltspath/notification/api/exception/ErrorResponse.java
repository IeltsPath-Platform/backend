package com.ieltspath.notification.api.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import java.util.Map;

public record ErrorResponse(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        String path,
        Map<String, String> details,
        @JsonInclude(JsonInclude.Include.NON_NULL) String code
) {
    public ErrorResponse(LocalDateTime timestamp, int status, String error, String message, String path,
                         Map<String, String> details) {
        this(timestamp, status, error, message, path, details, null);
    }
}

