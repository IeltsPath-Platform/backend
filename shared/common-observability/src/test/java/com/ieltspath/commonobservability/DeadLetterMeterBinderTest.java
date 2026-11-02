package com.ieltspath.commonobservability;

import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeadLetterMeterBinderTest {
    @Test
    void publishesOneGaugePerQueue() throws Exception {
        var registry = new SimpleMeterRegistry();
        binder(List.of("events.dlq"), () -> Map.of("events.dlq", 3L)).bindTo(registry);

        assertEquals(3.0, registry.get("dead_letter_messages").tag("queue", "events.dlq").gauge().value());
    }

    @Test
    void reportsNaNWhenBrokerCountFails() throws Exception {
        var registry = new SimpleMeterRegistry();
        binder(List.of("events.dlq"), () -> { throw new IllegalStateException("broker down"); }).bindTo(registry);

        assertTrue(Double.isNaN(registry.get("dead_letter_messages").tag("queue", "events.dlq").gauge().value()));
    }

    @SuppressWarnings("unchecked")
    private static MeterBinder binder(List<String> queues, Supplier<Map<String, Long>> counts) throws Exception {
        try {
            Class<?> type = Class.forName("com.ieltspath.commonobservability.DeadLetterMeterBinder");
            return (MeterBinder) type.getConstructor(List.class, Supplier.class).newInstance(queues, counts);
        } catch (InvocationTargetException failure) {
            throw new AssertionError(failure.getCause());
        }
    }
}
