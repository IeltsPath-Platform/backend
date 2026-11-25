package com.ieltspath.user.infrastructure.metrics;

import com.ieltspath.user.application.port.AuthMetrics;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MicrometerAuthMetrics implements AuthMetrics {
    private final MeterRegistry registry;

    @Override
    public void record(String metric, String tagName, String tagValue) {
        registry.counter(metric, tagName, tagValue).increment();
    }

    @Override
    public void record(String metric, String firstTag, String firstValue, String secondTag, String secondValue) {
        registry.counter(metric, firstTag, firstValue, secondTag, secondValue).increment();
    }

    @Override
    public void record(String metric) {
        registry.counter(metric).increment();
    }
}
