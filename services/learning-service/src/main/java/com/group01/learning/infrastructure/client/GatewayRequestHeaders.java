package com.group01.learning.infrastructure.client;

import com.group01.commonsecurity.header.SecurityHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;
import java.util.UUID;

/** The current request's gateway-signed JWT and correlation id, forwarded on every downstream call. */
final class GatewayRequestHeaders {
    private GatewayRequestHeaders() {}

    static Optional<String> bearerToken() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken token && authentication.isAuthenticated()) {
            return Optional.of(token.getToken().getTokenValue());
        }
        return Optional.empty();
    }

    /** The caller's id when it is short printable ASCII, otherwise a fresh one. */
    static String correlationId() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            String value = attributes.getRequest().getHeader(SecurityHeaders.CORRELATION_ID);
            if (value != null && !value.isBlank() && value.length() <= 128
                    && value.chars().allMatch(character -> character >= 32 && character <= 126)) {
                return value;
            }
        }
        return UUID.randomUUID().toString();
    }
}
