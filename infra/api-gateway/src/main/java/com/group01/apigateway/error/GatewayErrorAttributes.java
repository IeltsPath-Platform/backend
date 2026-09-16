package com.group01.apigateway.error;

import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.reactive.error.DefaultErrorAttributes;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class GatewayErrorAttributes extends DefaultErrorAttributes {
    @Override
    public Map<String, Object> getErrorAttributes(ServerRequest request, ErrorAttributeOptions options) {
        Map<String, Object> defaultAttributes = super.getErrorAttributes(request, options);
        int status = status(defaultAttributes);
        String error = reasonPhrase(status, defaultAttributes);

        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("timestamp", LocalDateTime.now());
        attributes.put("status", status);
        attributes.put("error", error);
        attributes.put("message", message(defaultAttributes, error));
        attributes.put("path", request.path());
        attributes.put("details", null);
        return attributes;
    }

    private int status(Map<String, Object> defaultAttributes) {
        Object status = defaultAttributes.get("status");
        return status instanceof Number number ? number.intValue() : HttpStatus.INTERNAL_SERVER_ERROR.value();
    }

    private String reasonPhrase(int statusCode, Map<String, Object> defaultAttributes) {
        HttpStatus status = HttpStatus.resolve(statusCode);
        if (status != null) {
            return reasonPhrase(status);
        }
        Object error = defaultAttributes.get("error");
        if (error instanceof String value && !value.isBlank()) {
            return value;
        }
        return "HTTP " + statusCode;
    }

    private String message(Map<String, Object> defaultAttributes, String fallback) {
        Object message = defaultAttributes.get("message");
        return message instanceof String value && !value.isBlank() ? value : fallback;
    }

    private String reasonPhrase(HttpStatus status) {
        return switch (status) {
            case BAD_REQUEST -> "Yêu cầu không hợp lệ";
            case UNAUTHORIZED -> "Chưa xác thực";
            case FORBIDDEN -> "Không có quyền truy cập";
            case NOT_FOUND -> "Không tìm thấy";
            case CONFLICT -> "Xung đột dữ liệu";
            case SERVICE_UNAVAILABLE -> "Dịch vụ tạm thời không khả dụng";
            case INTERNAL_SERVER_ERROR -> "Lỗi hệ thống";
            default -> status.getReasonPhrase();
        };
    }
}
