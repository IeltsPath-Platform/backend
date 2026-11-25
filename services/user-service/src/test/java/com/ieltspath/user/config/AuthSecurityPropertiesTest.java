package com.ieltspath.user.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThat;

class AuthSecurityPropertiesTest {
    @Test
    void secureCookieEnforcementAcceptsLocalAndSecureConfigurations() {
        assertThatCode(() -> new AuthCookieSecurityGuard(new AuthCookieProperties(
                true, "access_token", "/", "refresh_token", "/auth", null, "Lax", false, true, false))).doesNotThrowAnyException();
        assertThatCode(() -> new AuthCookieSecurityGuard(new AuthCookieProperties(
                true, "access_token", "/", "refresh_token", "/auth", null, "Lax", true, true, true))).doesNotThrowAnyException();
    }

    @Test
    void secureCookieEnforcementRejectsNonSecureCookieConfiguration() {
        assertThatThrownBy(() -> new AuthCookieSecurityGuard(new AuthCookieProperties(
                true, "access_token", "/", "refresh_token", "/auth", null, "Lax", false, true, true)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Cookie xác thực phải bật Secure khi enforce-secure được bật.");
    }

    @Test
    void passwordHashCostMustStayWithinSupportedBounds() {
        assertThatThrownBy(() -> new AuthPasswordProperties(8, 9)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AuthPasswordProperties(7, 12)).isInstanceOf(IllegalArgumentException.class);
        assertThatCode(() -> new AuthPasswordProperties(8, 12)).doesNotThrowAnyException();
    }

    @Test
    void passwordEncoderUsesTheConfiguredBcryptCost() {
        var encoder = new AuthTokenConfig().passwordEncoder(new AuthPasswordProperties(8, 12));
        assertThat(encoder.encode("R4ndom!x")).startsWith("$2a$12$");
    }
}
