package com.group01.user.api.cookie;

import com.group01.user.config.AuthCookieProperties;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthCookieService {
    private final AuthCookieProperties properties;

    public void addAuthCookies(HttpServletResponse response, String accessToken, String refreshToken,
                               long accessMaxAgeSeconds, long refreshMaxAgeSeconds) {
        if (!properties.enabled()) {
            return;
        }
        ResponseCookie accessCookie = buildCookie(
                properties.accessTokenName(),
                accessToken,
                properties.accessTokenPath(),
                accessMaxAgeSeconds
        );
        ResponseCookie refreshCookie = buildCookie(
                properties.refreshTokenName(),
                refreshToken,
                properties.refreshTokenPath(),
                refreshMaxAgeSeconds
        );
        response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());
    }

    public void clearCookies(HttpServletResponse response) {
        if (!properties.enabled()) {
            return;
        }
        ResponseCookie accessCookie = buildCookie(
                properties.accessTokenName(),
                "",
                properties.accessTokenPath(),
                0
        );
        ResponseCookie refreshCookie = buildCookie(
                properties.refreshTokenName(),
                "",
                properties.refreshTokenPath(),
                0
        );
        response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());
    }

    public String getRefreshTokenName() {
        return properties.refreshTokenName();
    }

    public String getAccessTokenName() {
        return properties.accessTokenName();
    }

    private ResponseCookie buildCookie(String name, String value, String path, long maxAge) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(name, value)
                .path(path)
                .maxAge(maxAge)
                .httpOnly(properties.httpOnly())
                .secure(properties.secure())
                .sameSite(properties.sameSite());

        if (properties.domain() != null && !properties.domain().isBlank()) {
            builder.domain(properties.domain());
        }

        return builder.build();
    }
}

