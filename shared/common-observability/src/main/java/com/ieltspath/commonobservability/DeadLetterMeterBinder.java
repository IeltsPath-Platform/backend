package com.ieltspath.commonobservability;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/** Exposes the current queue depth of each declared dead-letter queue. */
public final class DeadLetterMeterBinder implements MeterBinder {
    private final List<String> queues;
    private final Supplier<Map<String, Long>> counts;

    public DeadLetterMeterBinder(List<String> queues, Supplier<Map<String, Long>> counts) {
        this.queues = List.copyOf(queues);
        this.counts = counts;
    }

    @Override
    public void bindTo(MeterRegistry registry) {
        for (String queue : queues) {
            registry.gauge("dead_letter_messages", io.micrometer.core.instrument.Tags.of("queue", queue),
                    queue, this::count);
        }
    }

    private double count(String queue) {
        try {
            return counts.get().getOrDefault(queue, 0L).doubleValue();
        } catch (RuntimeException brokerFailure) {
            return Double.NaN;
        }
    }
}
