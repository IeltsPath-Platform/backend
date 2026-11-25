package com.ieltspath.user.infrastructure.metrics;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuthMetricsTest {
    @Test
    void incrementsAuthenticationCountersUsingOnlyBoundedResultAndMethodTags() {
        var registry = new SimpleMeterRegistry();
        var metrics = new MicrometerAuthMetrics(registry);
        metrics.record("auth.reauth", "result", "success", "method", "GOOGLE");
        metrics.record("auth.login", "result", "success");

        assertThat(registry.get("auth.reauth").tag("result", "success").tag("method", "GOOGLE").counter().count()).isEqualTo(1);
        assertThat(registry.get("auth.login").tag("result", "success").counter().count()).isEqualTo(1);
    }
}
