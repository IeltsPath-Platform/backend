package com.ieltspath.apigateway.filter;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class WebSocketProtocolAuthFilterTest {
    private final WebSocketProtocolAuthFilter filter = new WebSocketProtocolAuthFilter();

    @Test void forwardsOnlyStompProtocolAndUsesCredentialAsBearerHeader() {
        var request=MockServerHttpRequest.get("/ws/notifications").header(HttpHeaders.UPGRADE,"websocket")
                .header("Sec-WebSocket-Protocol","v12.stomp, bearer.external-token").build();
        var exchange=MockServerWebExchange.from(request);
        filter.filter(exchange, sanitized -> {
            assertEquals("Bearer external-token",sanitized.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION));
            assertEquals("v12.stomp",sanitized.getRequest().getHeaders().getFirst("Sec-WebSocket-Protocol"));
            return reactor.core.publisher.Mono.empty();
        }).block();
    }

    @Test void rejectsQueryTokenAndMissingSubprotocol() {
        var request=MockServerHttpRequest.get("/ws/notifications?token=external-token").header(HttpHeaders.UPGRADE,"websocket").build();
        var exchange=MockServerWebExchange.from(request); var reached=new AtomicBoolean();
        filter.filter(exchange, ignored -> { reached.set(true); return reactor.core.publisher.Mono.empty(); }).block();
        assertEquals(org.springframework.http.HttpStatus.UNAUTHORIZED,exchange.getResponse().getStatusCode());
        assertFalse(reached.get());
    }
}
