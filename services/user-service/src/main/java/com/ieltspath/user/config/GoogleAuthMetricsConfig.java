package com.ieltspath.user.config;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class GoogleAuthMetricsConfig {
    public GoogleAuthMetricsConfig(GoogleAuthProperties properties, MeterRegistry registry) {
        boolean configured = properties.getClientIds().stream().anyMatch(value -> value != null && !value.isBlank());
        Gauge.builder("auth.google.configured", () -> configured ? 1 : 0)
                .register(registry);
        if (!configured) {
            log.warn("Google sign-in tắt: chưa đặt GOOGLE_CLIENT_IDS");
        }
    }
}
