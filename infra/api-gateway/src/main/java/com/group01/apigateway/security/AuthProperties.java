package com.group01.apigateway.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app.auth")
public record AuthProperties(
        String externalJwtIssuer,
        String externalJwtSecret,
        String internalJwtIssuer,
        String internalJwtSecret,
        long internalTokenMaxAgeSeconds,
        List<String> internalJwtPaths,
        String frontendOrigin
) {
    public AuthProperties {
        if (externalJwtIssuer == null || externalJwtIssuer.isBlank()) {
            externalJwtIssuer = "urn:code-base:auth";
        }
        requireConfigured(externalJwtSecret, "app.auth.external-jwt-secret");
        if (internalJwtIssuer == null || internalJwtIssuer.isBlank()) {
            internalJwtIssuer = "urn:code-base:api-gateway";
        }
        requireConfigured(internalJwtSecret, "app.auth.internal-jwt-secret");
        if (internalTokenMaxAgeSeconds <= 0 || internalTokenMaxAgeSeconds > 300) {
            internalTokenMaxAgeSeconds = 60;
        }
        if (internalJwtPaths == null || internalJwtPaths.isEmpty()) {
            internalJwtPaths = List.of("/api/users", "/auth/me");
        } else {
            internalJwtPaths = List.copyOf(internalJwtPaths);
        }
        if (frontendOrigin == null || frontendOrigin.isBlank()) {
            frontendOrigin = "http://localhost:5173";
        }
    }

    private static void requireConfigured(String value, String property) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(property + " must be configured");
        }
    }
}
