package com.ieltspath.user.infrastructure.metrics;

import com.ieltspath.user.application.port.GoogleSignInMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuthenticationMetricsTest {
    @Test
    void recordsGoogleSignInResultsAndPasswordHashUpgrades() {
        var registry = new SimpleMeterRegistry();
        var googleMetrics = new MicrometerGoogleSignInMetrics(registry);
        var rehashMetrics = new MicrometerPasswordRehashMetrics(registry);

        for (GoogleSignInMetrics.Result result : GoogleSignInMetrics.Result.values()) googleMetrics.record(result);
        rehashMetrics.recordUpgrade();

        assertThat(registry.get("auth.google.signin").tag("result", "success").counter().count()).isEqualTo(1);
        assertThat(registry.get("auth.google.signin").tag("result", "invalid_token").counter().count()).isEqualTo(1);
        assertThat(registry.get("auth.google.signin").tag("result", "email_not_supported").counter().count()).isEqualTo(1);
        assertThat(registry.get("auth.google.signin").tag("result", "not_allowed").counter().count()).isEqualTo(1);
        assertThat(registry.get("auth.google.signin").tag("result", "conflict").counter().count()).isEqualTo(1);
        assertThat(registry.get("auth.google.signin").tag("result", "inactive").counter().count()).isEqualTo(1);
        assertThat(registry.get("auth.google.signin").tag("result", "unavailable").counter().count()).isEqualTo(1);
        assertThat(registry.get("auth.password").tag("event", "rehash").counter().count()).isEqualTo(1);
    }
}
