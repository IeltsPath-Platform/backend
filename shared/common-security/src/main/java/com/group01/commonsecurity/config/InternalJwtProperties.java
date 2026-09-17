package com.group01.commonsecurity.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.auth")
public record InternalJwtProperties(
        @NotBlank String internalJwtIssuer,
        @NotBlank String internalJwtSecret
) {
    public InternalJwtProperties {
        requireConfigured(internalJwtIssuer, "app.auth.internal-jwt-issuer");
        requireConfigured(internalJwtSecret, "app.auth.internal-jwt-secret");
    }

    private static void requireConfigured(String value, String property) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(property + " must be configured");
        }
    }
}
