package com.ieltspath.user.infrastructure.metrics;

import com.ieltspath.user.application.port.GoogleSignInMetrics;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MicrometerGoogleSignInMetrics implements GoogleSignInMetrics {
    private final MeterRegistry registry;

    @Override
    public void record(Result result) {
        registry.counter("auth.google.signin", "result", result.tag()).increment();
    }
}
