package com.ieltspath.commonobservability;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * An outgoing call made inside a span carries that span's trace in the W3C traceparent header, and the logging
 * context shows the same trace id while the span is active.
 */
@AutoConfigureObservability
@SpringBootTest(classes = TracePropagationTest.Application.class, properties = {
        "management.tracing.enabled=true",
        "app.auth.internal-jwt-issuer=urn:test:gateway",
        "app.auth.internal-jwt-secret=dGVzdC1vbmx5LWludGVybmFsLWp3dC1zaWduaW5nLWtleQ=="
})
class TracePropagationTest {

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class Application { }

    @Autowired RestClient.Builder builder;
    @Autowired Tracer tracer;
    @Autowired OutboxTraceContext outboxTraceContext;

    @Test
    void outgoingRequestCarriesTheTraceOfTheActiveSpan() {
        Span span = tracer.nextSpan().name("outbound-test").start();
        String traceId = span.context().traceId();

        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClient client = builder.baseUrl("http://downstream.test").build();
        server.expect(requestTo("http://downstream.test/ping"))
                .andExpect(request -> {
                    String traceparent = request.getHeaders().getFirst("traceparent");
                    assertNotNull(traceparent, "outgoing request has no traceparent header");
                    // W3C format: version-traceid-spanid-flags; the trace id must be the active span's.
                    assertEquals(traceId, traceparent.split("-")[1], "traceparent carries the active trace id");
                })
                .andRespond(withSuccess("pong", null));

        try (Tracer.SpanInScope ignored = tracer.withSpan(span)) {
            assertEquals(traceId, MDC.get("traceId"), "logging context shows the active trace");
            client.get().uri("/ping").retrieve().body(String.class);
        } finally {
            span.end();
        }

        server.verify();
    }

    @Test
    void outboxSnapshotRestoresTheRequestTraceForAChildRelaySpan() {
        Span request = tracer.nextSpan().name("request").start();
        String traceId = request.context().traceId();
        String requestSpanId = request.context().spanId();

        try (Tracer.SpanInScope ignored = tracer.withSpan(request)) {
            String snapshot = outboxTraceContext.capture();
            assertNotNull(snapshot);

            try (OutboxTraceContext.Scope relay = outboxTraceContext.openRelay(snapshot)) {
                assertEquals(traceId, relay.span().context().traceId());
                org.junit.jupiter.api.Assertions.assertNotEquals(requestSpanId, relay.span().context().spanId());
                assertEquals(relay.span().context().spanId(), tracer.currentSpan().context().spanId());
            }
        } finally {
            request.end();
        }
    }

    @Test
    void noActiveSpanProducesNoSnapshot() {
        assertEquals(null, outboxTraceContext.capture());
    }
}
