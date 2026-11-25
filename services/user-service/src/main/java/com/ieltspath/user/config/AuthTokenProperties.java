package com.ieltspath.user.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.auth")
public record AuthTokenProperties(
        @NotBlank String externalJwtIssuer,
        @NotBlank String externalJwtSecret,
        long accessTokenMaxAgeSeconds,
        long refreshTokenMaxAgeSeconds,
        long refreshReuseGraceSeconds,
        long sessionMaxAgeSeconds
) {
    public AuthTokenProperties {
        requireConfigured(externalJwtIssuer, "app.auth.external-jwt-issuer");
        requireConfigured(externalJwtSecret, "app.auth.external-jwt-secret");
        if (accessTokenMaxAgeSeconds <= 0) {
            accessTokenMaxAgeSeconds = 3600;
        }
        if (refreshTokenMaxAgeSeconds <= 0) {
            refreshTokenMaxAgeSeconds = 604800;
        }
        if (refreshReuseGraceSeconds <= 0) {
            refreshReuseGraceSeconds = 10;
        }
        if (sessionMaxAgeSeconds < 0) {
            sessionMaxAgeSeconds = 0;
        }
    }

    private static void requireConfigured(String value, String property) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(property + " must be configured");
        }
    }
}
