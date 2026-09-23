package com.group01.user.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.auth.cookie")
public record AuthCookieProperties(
        boolean enabled,
        String accessTokenName,
        String accessTokenPath,
        String refreshTokenName,
        String refreshTokenPath,
        String domain,
        String sameSite,
        boolean secure,
        boolean httpOnly
) {
    public AuthCookieProperties {
        if (accessTokenName == null || accessTokenName.isBlank()) {
            accessTokenName = "access_token";
        }
        if (accessTokenPath == null || accessTokenPath.isBlank()) {
            accessTokenPath = "/";
        }
        if (refreshTokenName == null || refreshTokenName.isBlank()) {
            refreshTokenName = "refresh_token";
        }
        if (refreshTokenPath == null || refreshTokenPath.isBlank()) {
            refreshTokenPath = "/auth";
        }
        if (domain != null && domain.isBlank()) {
            domain = null;
        }
        if (sameSite == null || sameSite.isBlank()) {
            sameSite = "Lax";
        }
    }

    public static AuthCookieProperties defaults() {
        return new AuthCookieProperties(true, "access_token", "/", "refresh_token", "/auth", null, "Lax", false, true);
    }
}

