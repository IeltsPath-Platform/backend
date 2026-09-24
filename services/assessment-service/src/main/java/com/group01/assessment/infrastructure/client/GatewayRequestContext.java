package com.group01.assessment.infrastructure.client;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

/** Propagates the caller's gateway-signed internal JWT and correlation id to downstream services. */
final class GatewayRequestContext {
    private GatewayRequestContext() {}

    static String bearerToken() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken jwtAuthenticationToken)
                || !authentication.isAuthenticated()) {
            throw new IllegalStateException("Authenticated gateway JWT is required to call a downstream service");
        }
        return "Bearer " + jwtAuthenticationToken.getToken().getTokenValue();
    }

    static String correlationId() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            String value = attributes.getRequest().getHeader("X-Correlation-Id");
            if (value != null && !value.isBlank() && value.length() <= 128) return value;
        }
        return UUID.randomUUID().toString();
    }
}
