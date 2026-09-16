package com.group01.user.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.auth")
public record AuthTokenProperties(
        @NotBlank String externalJwtIssuer,
        @NotBlank String externalJwtSecret,
        @NotBlank String internalJwtIssuer,
        @NotBlank String internalJwtSecret,
        long accessTokenMaxAgeSeconds,
        long refreshTokenMaxAgeSeconds
) {
    public AuthTokenProperties {
        requireConfigured(externalJwtIssuer, "app.auth.external-jwt-issuer");
        requireConfigured(externalJwtSecret, "app.auth.external-jwt-secret");
        requireConfigured(internalJwtIssuer, "app.auth.internal-jwt-issuer");
        requireConfigured(internalJwtSecret, "app.auth.internal-jwt-secret");
        if (accessTokenMaxAgeSeconds <= 0) {
            accessTokenMaxAgeSeconds = 3600;
        }
        if (refreshTokenMaxAgeSeconds <= 0) {
            refreshTokenMaxAgeSeconds = 604800;
        }
    }

    private static void requireConfigured(String value, String property) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(property + " must be configured");
        }
    }
}
