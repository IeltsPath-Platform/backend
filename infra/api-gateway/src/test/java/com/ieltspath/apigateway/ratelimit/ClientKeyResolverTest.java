package com.ieltspath.apigateway.ratelimit;

import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ClientKeyResolverTest {
    @Test
    void usesFirstUntrustedAddressFromTheRight() {
        ClientKeyResolver resolver = resolver(List.of("10.0.0.1"));
        ServerWebExchange exchange = exchange("6.6.6.6, 198.51.100.7", "10.0.0.1");

        assertThat(resolver.resolve(exchange).block()).isEqualTo("ip:198.51.100.7");
    }

    @Test
    void skipsTrustedHopsFromTheRight() {
        ClientKeyResolver resolver = resolver(List.of("10.0.0.1", "10.0.0.2"));
        ServerWebExchange exchange = exchange("6.6.6.6, 198.51.100.7, 10.0.0.2", "10.0.0.1");

        assertThat(resolver.resolve(exchange).block()).isEqualTo("ip:198.51.100.7");
    }

    @Test
    void fallsBackToRemoteWhenEveryHopIsTrusted() {
        ClientKeyResolver resolver = resolver(List.of("10.0.0.1", "10.0.0.2"));
        ServerWebExchange exchange = exchange("10.0.0.2", "10.0.0.1");

        assertThat(resolver.resolve(exchange).block()).isEqualTo("ip:10.0.0.1");
    }

    @Test
    void ignoresSpoofedLeftmostEntry() {
        ClientKeyResolver resolver = resolver(List.of("10.0.0.1", "10.0.0.2"));
        ServerWebExchange exchange = exchange("6.6.6.6, 203.0.113.8, 10.0.0.2", "10.0.0.1");

        assertThat(resolver.resolve(exchange).block()).isEqualTo("ip:203.0.113.8");
    }

    private ClientKeyResolver resolver(List<String> trustedProxies) {
        return new ClientKeyResolver(new RateLimitProperties(trustedProxies, Map.of()));
    }

    private ServerWebExchange exchange(String forwardedFor, String remoteAddress) {
        return MockServerWebExchange.from(MockServerHttpRequest.get("/")
                .header("X-Forwarded-For", forwardedFor)
                .remoteAddress(new InetSocketAddress(remoteAddress, 1234)));
    }
}
