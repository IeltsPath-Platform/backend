package com.group01.apigateway.error;

import java.time.LocalDateTime;
import java.util.Map;

public record GatewayErrorResponse(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        String path,
        Map<String, String> details
) {
}
