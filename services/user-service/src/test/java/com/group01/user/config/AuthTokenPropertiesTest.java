package com.group01.user.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthTokenPropertiesTest {

    @Test
    void failsFastWhenExternalJwtSecretIsMissing() {
        assertThatThrownBy(() -> new AuthTokenProperties(
                "urn:code-base:auth",
                "",
                3600,
                604800))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("app.auth.external-jwt-secret must be configured");
    }
}
