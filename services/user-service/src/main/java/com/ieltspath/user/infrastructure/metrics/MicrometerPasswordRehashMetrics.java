package com.ieltspath.user.infrastructure.metrics;

import com.ieltspath.user.application.port.PasswordRehashMetrics;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MicrometerPasswordRehashMetrics implements PasswordRehashMetrics {
    private final MeterRegistry registry;

    @Override
    public void recordUpgrade() {
        registry.counter("auth.password.rehash").increment();
    }
}
