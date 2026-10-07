package com.ieltspath.game.infrastructure.client;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.client.RestClient;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ContentSnapshotClientTest {
    private HttpServer contentServer;
    private HttpServer libraryServer;

    @AfterEach
    void cleanUp() {
        SecurityContextHolder.clearContext();
        if (contentServer != null) contentServer.stop(0);
        if (libraryServer != null) libraryServer.stop(0);
    }

    @Test
    void routesVocabularyToLibraryAndGrammarToContent() throws Exception {
        AtomicInteger contentCalls = new AtomicInteger();
        AtomicInteger libraryCalls = new AtomicInteger();
        contentServer = server(contentCalls);
        libraryServer = server(libraryCalls);
        var jwt = Jwt.withTokenValue("test-internal-token")
                .header("alg", "HS256").subject(UUID.randomUUID().toString())
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60)).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, List.of()));
        var client = new ContentSnapshotClient(RestClient.builder(),
                "http://localhost:" + contentServer.getAddress().getPort(),
                "http://localhost:" + libraryServer.getAddress().getPort());

        assertTrue(client.loadSnapshot("SPELLING", "VOCABULARY", List.of(UUID.randomUUID())).isEmpty());
        assertEquals(0, contentCalls.get());
        assertEquals(1, libraryCalls.get());

        assertTrue(client.loadSnapshot("WORD_ORDER", "GRAMMAR", List.of(UUID.randomUUID())).isEmpty());
        assertEquals(1, contentCalls.get());
        assertEquals(1, libraryCalls.get());
    }

    private HttpServer server(AtomicInteger calls) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/internal/game-content/snapshots", exchange -> {
            calls.incrementAndGet();
            assertEquals("Bearer test-internal-token", exchange.getRequestHeaders().getFirst("Authorization"));
            assertNotNull(exchange.getRequestHeaders().getFirst("X-Correlation-Id"));
            exchange.getRequestBody().readAllBytes();
            byte[] body = "{\"items\":[]}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        return server;
    }
}
