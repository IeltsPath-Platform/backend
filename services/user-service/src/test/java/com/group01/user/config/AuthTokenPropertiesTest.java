package com.group01.user.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthTokenPropertiesTest {

    @Test
    void failsFastWhenInternalJwtSecretIsMissing() {
        assertThatThrownBy(() -> new AuthTokenProperties(
                "urn:code-base:auth",
                "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
                "urn:code-base:api-gateway",
                "",
                3600,
                604800))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("app.auth.internal-jwt-secret must be configured");
    }
}
