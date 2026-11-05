package com.ieltspath.game.infrastructure.client;

import com.ieltspath.commonresilience.ResilientCall;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.client.RestClient;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
class WordlyWordClientTest {
    private static final UUID WORD_ID = UUID.fromString("f3e62a04-9d62-4d80-a99f-11bec943517d");

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void retriesTemporaryLibraryFailureAndReturnsTheWord() throws IOException {
        AtomicInteger requests = new AtomicInteger();
        AtomicReference<String> authorization = new AtomicReference<>();
        HttpServer server = libraryServer(requests, authorization, 1);
        authenticate();
        try {
            var words = client(server).random(1);

            assertThat(words).containsExactly(new com.ieltspath.game.application.port.WordlyWordProvider.Word(
                    WORD_ID, "resilient", "bền bỉ"));
            assertThat(requests).hasValue(2);
            assertThat(authorization).hasValue("Bearer reliability-token");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void rethrowsOriginalLibraryFailureAfterRetriesAreExhausted() throws IOException {
        AtomicInteger requests = new AtomicInteger();
        HttpServer server = libraryServer(requests, new AtomicReference<>(), 3);
        authenticate();
        try {
            assertThatThrownBy(() -> client(server).random(1))
                    .isInstanceOf(org.springframework.web.client.HttpServerErrorException.ServiceUnavailable.class);
            assertThat(requests).hasValue(2);
        } finally {
            server.stop(0);
        }
    }

    private static WordlyWordClient client(HttpServer server) {
        return new WordlyWordClient(RestClient.builder(),
                "http://127.0.0.1:" + server.getAddress().getPort(),
                ResilientCall.isolatedDefault());
    }

    private static HttpServer libraryServer(AtomicInteger requests, AtomicReference<String> authorization,
                                            int failuresBeforeSuccess) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/internal/game-content/wordly/random", exchange -> {
            authorization.set(exchange.getRequestHeaders().getFirst(HttpHeaders.AUTHORIZATION));
            int attempt = requests.incrementAndGet();
            int status = attempt <= failuresBeforeSuccess ? 503 : 200;
            String body = status == 200 ? "{\"words\":[{\"sourceWordId\":\"" + WORD_ID
                    + "\",\"answer\":\"resilient\",\"meaning\":\"bền bỉ\"}]}" : "{}";
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            try (var output = exchange.getResponseBody()) {
                output.write(bytes);
            }
        });
        server.start();
        return server;
    }

    private static void authenticate() {
        Jwt jwt = Jwt.withTokenValue("reliability-token").header("alg", "none")
                .subject(UUID.randomUUID().toString()).build();
        JwtAuthenticationToken authentication = new JwtAuthenticationToken(jwt);
        authentication.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
