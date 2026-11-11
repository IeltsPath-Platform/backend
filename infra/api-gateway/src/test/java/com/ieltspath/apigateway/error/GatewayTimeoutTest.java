package com.ieltspath.apigateway.error;

import org.junit.jupiter.api.Test;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.reactive.error.DefaultErrorAttributes;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.reactive.function.server.HandlerStrategies;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class GatewayTimeoutTest {
    @Test
    void slowDownstreamReturnsGatewayTimeout() {
        var exchange = MockServerWebExchange.from(org.springframework.mock.http.server.reactive.MockServerHttpRequest
                .get("/api/learning/free-writing/submissions"));
        var errors = new DefaultErrorAttributes();
        errors.storeErrorInformation(new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT,
                "downstream timed out"), exchange);
        ServerRequest request = ServerRequest.create(exchange, HandlerStrategies.withDefaults().messageReaders());

        var attributes = new GatewayErrorAttributes().getErrorAttributes(request, ErrorAttributeOptions.defaults());

        assertEquals(504, attributes.get("status"));
        assertEquals("GATEWAY_TIMEOUT", attributes.get("code"));
        assertEquals("Gateway chờ dịch vụ quá thời gian", attributes.get("error"));
    }

    @Test
    void aiRouteAllowsLongerResponse() throws Exception {
        String yaml = Files.readString(Path.of("..", "..", "infra", "config-server", "config-repo", "api-gateway.yaml"));
        int route = yaml.indexOf("- id: learning-ai-writing-limited");
        assertTrue(route >= 0);
        String routeBlock = yaml.substring(route, yaml.indexOf("- id: learning-service", route));
        // Route metadata is read as a number of milliseconds; "60s" is not a valid value there.
        assertTrue(routeBlock.contains("response-timeout: 60000"));
    }

    @Test
    void retriesGetOnceOnConnectionRefused() throws Exception {
        String yaml = Files.readString(Path.of("..", "..", "infra", "config-server", "config-repo", "api-gateway.yaml"));
        int retry = yaml.indexOf("- name: Retry");
        assertTrue(retry >= 0);
        String block = yaml.substring(retry, yaml.indexOf("routes:", retry));
        assertTrue(block.contains("retries: 1"));
        assertTrue(block.contains("methods: GET"));
        assertTrue(block.contains("java.net.ConnectException"));
    }

    @Test
    void neverRetriesPost() throws Exception {
        String yaml = Files.readString(Path.of("..", "..", "infra", "config-server", "config-repo", "api-gateway.yaml"));
        int retry = yaml.indexOf("- name: Retry");
        int routes = yaml.indexOf("routes:", retry);
        String block = yaml.substring(retry, routes);
        assertTrue(block.contains("methods: GET"));
        assertFalse(block.contains("methods: POST"));
    }
}
