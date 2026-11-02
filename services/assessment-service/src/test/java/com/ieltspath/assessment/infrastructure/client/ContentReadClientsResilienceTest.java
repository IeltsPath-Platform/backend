package com.ieltspath.assessment.infrastructure.client;

import com.ieltspath.assessment.application.exception.ContentUnavailableException;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ContentReadClientsResilienceTest {
    private HttpServer server;
    private AtomicInteger calls;
    private String contentUrl;

    @BeforeEach
    void startServer() throws IOException {
        calls = new AtomicInteger();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            calls.incrementAndGet();
            exchange.sendResponseHeaders(404, -1);
            exchange.close();
        });
        server.start();
        contentUrl = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void rubricNotFoundKeepsItsUnavailableMappingWithoutRetry() {
        ContentRubricClient client = new ContentRubricClient(RestClient.builder(), contentUrl, tokenProvider());

        assertThrows(ContentUnavailableException.class, () -> client.getActive("WRITING"));

        assertEquals(1, calls.get());
    }

    @Test
    void errorTypeNotFoundKeepsItsUnavailableMappingWithoutRetry() {
        ContentErrorTypeClient client = new ContentErrorTypeClient(RestClient.builder(), contentUrl, tokenProvider());

        assertThrows(ContentUnavailableException.class, client::activeErrorTypes);

        assertEquals(1, calls.get());
    }

    private static AssessmentServiceTokenProvider tokenProvider() {
        AssessmentServiceTokenProvider tokens = mock(AssessmentServiceTokenProvider.class);
        when(tokens.authorizationHeader()).thenReturn("Bearer test-token");
        return tokens;
    }
}
