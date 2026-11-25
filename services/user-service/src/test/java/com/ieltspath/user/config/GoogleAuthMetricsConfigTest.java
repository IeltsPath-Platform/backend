package com.ieltspath.user.config;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GoogleAuthMetricsConfigTest {
    @Test
    void exposesWhetherGoogleSignInIsConfiguredWithoutExposingClientIds() {
        var empty = new GoogleAuthProperties();
        var emptyRegistry = new SimpleMeterRegistry();
        new GoogleAuthMetricsConfig(empty, emptyRegistry);
        assertThat(emptyRegistry.get("auth.google.configured").gauge().value()).isZero();

        var configured = new GoogleAuthProperties();
        configured.setClientIds(List.of("private-client-id"));
        var configuredRegistry = new SimpleMeterRegistry();
        new GoogleAuthMetricsConfig(configured, configuredRegistry);
        assertThat(configuredRegistry.get("auth.google.configured").gauge().value()).isEqualTo(1);
    }
}
