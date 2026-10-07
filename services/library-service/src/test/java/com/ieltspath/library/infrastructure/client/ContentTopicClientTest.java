package com.ieltspath.library.infrastructure.client;

import com.ieltspath.library.application.exception.TopicServiceUnavailableException;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class ContentTopicClientTest {
    private HttpServer server;

    @AfterEach
    void cleanUp() {
        if (server != null) server.stop(0);
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void forwardsJwtAndCorrelationAndMapsDownstreamResults() throws Exception {
        AtomicInteger status = new AtomicInteger(200);
        AtomicReference<String> authorization = new AtomicReference<>();
        AtomicReference<String> correlation = new AtomicReference<>();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/content/topics/", exchange -> {
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            correlation.set(exchange.getRequestHeaders().getFirst("X-Correlation-Id"));
            exchange.sendResponseHeaders(status.get(), -1);
            exchange.close();
        });
        server.start();
        var jwt = Jwt.withTokenValue("test-internal-token").header("alg", "HS256")
                .subject(UUID.randomUUID().toString()).issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60)).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, List.of()));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Correlation-Id", "topic-check-123");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        var client = new ContentTopicClient(RestClient.builder(),
                "http://localhost:" + server.getAddress().getPort());

        assertTrue(client.exists(UUID.randomUUID()));
        assertEquals("Bearer test-internal-token", authorization.get());
        assertEquals("topic-check-123", correlation.get());
        status.set(404);
        assertFalse(client.exists(UUID.randomUUID()));
        status.set(503);
        assertThrows(TopicServiceUnavailableException.class, () -> client.exists(UUID.randomUUID()));
    }
}
