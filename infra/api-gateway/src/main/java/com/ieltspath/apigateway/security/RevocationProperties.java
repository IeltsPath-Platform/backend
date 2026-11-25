package com.ieltspath.apigateway.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gateway.auth.revocation")
public record RevocationProperties(boolean failClosed) {
}
