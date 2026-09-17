package com.group01.commonsecurity.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InternalJwtPropertiesTest {
    @Test
    void failsFastWhenInternalJwtSecretIsMissing() {
        assertThatThrownBy(() -> new InternalJwtProperties(
                "urn:code-base:api-gateway",
                ""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("app.auth.internal-jwt-secret must be configured");
    }
}
