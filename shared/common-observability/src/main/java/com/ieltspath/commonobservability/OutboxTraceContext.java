package com.ieltspath.commonobservability;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;
import org.springframework.beans.factory.ObjectProvider;

import java.util.HashMap;
import java.util.Map;

/** Captures and restores the W3C trace headers needed to link an outbox relay to its originating request. */
public final class OutboxTraceContext {
    private static final String TRACEPARENT = "traceparent";
    private static final String TRACESTATE = "tracestate";
    private static final String SEPARATOR = "\n";

    private final ObjectProvider<Tracer> tracerProvider;
    private final ObjectProvider<Propagator> propagatorProvider;

    public OutboxTraceContext(ObjectProvider<Tracer> tracerProvider, ObjectProvider<Propagator> propagatorProvider) {
        this.tracerProvider = tracerProvider;
        this.propagatorProvider = propagatorProvider;
    }

    /** Returns null when tracing is unavailable or no span is active. */
    public String capture() {
        Tracer tracer = tracerProvider.getIfAvailable();
        Propagator propagator = propagatorProvider.getIfAvailable();
        Span current = tracer == null ? null : tracer.currentSpan();
        if (current == null || propagator == null) return null;

        Map<String, String> headers = new HashMap<>();
        propagator.inject(current.context(), headers, Map::put);
        String traceparent = headers.get(TRACEPARENT);
        if (traceparent == null || traceparent.isBlank()) return null;
        return traceparent + SEPARATOR + headers.getOrDefault(TRACESTATE, "");
    }

    /** Opens an {@code outbox.relay} child span from a stored snapshot, or a new root span for a legacy row. */
    public Scope openRelay(String snapshot) {
        Tracer tracer = tracerProvider.getIfAvailable();
        Propagator propagator = propagatorProvider.getIfAvailable();
        if (tracer == null || propagator == null) return Scope.NOOP;

        Span span;
        if (snapshot == null || snapshot.isBlank()) {
            span = tracer.nextSpan().name("outbox.relay").start();
        } else {
            String[] parts = snapshot.split(SEPARATOR, 2);
            Map<String, String> headers = new HashMap<>();
            headers.put(TRACEPARENT, parts[0]);
            if (parts.length == 2 && !parts[1].isBlank()) headers.put(TRACESTATE, parts[1]);
            span = propagator.extract(headers, Map::get).name("outbox.relay").start();
        }
        return new Scope(tracer, span);
    }

    public static final class Scope implements AutoCloseable {
        private static final Scope NOOP = new Scope(null, null);
        private final Span span;
        private final Tracer.SpanInScope scope;

        private Scope(Tracer tracer, Span span) {
            this.span = span;
            this.scope = span == null ? null : tracer.withSpan(span);
        }

        public Span span() {
            return span;
        }

        @Override
        public void close() {
            if (scope != null) {
                try {
                    scope.close();
                } finally {
                    span.end();
                }
            }
        }
    }
}
