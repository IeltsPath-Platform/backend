package com.ieltspath.apigateway.ratelimit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.server.ServerWebExchange;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ClientRateLimitGatewayFilterFactoryTest {
    private RedisRateLimiter limiter;
    private ClientKeyResolver keys;
    private RateLimitProperties properties;
    private SimpleMeterRegistry meters;

    @BeforeEach
    void setUp() {
        limiter = mock(RedisRateLimiter.class);
        keys = mock(ClientKeyResolver.class);
        properties = new RateLimitProperties(List.of(), Map.of(
                "auth", new RateLimitProperties.Tier(1, 5),
                "upload", new RateLimitProperties.Tier(1, 5),
                "ai", new RateLimitProperties.Tier(1, 3),
                "default", new RateLimitProperties.Tier(20, 40)));
        meters = new SimpleMeterRegistry();
    }

    @Test
    void rejectsWithRetryAfterWhenBurstExceeded() {
        when(keys.resolve(org.mockito.ArgumentMatchers.any())).thenReturn(Mono.just("ip:203.0.113.9"));
        when(limiter.isAllowed(anyString(), anyString()))
                .thenReturn(Mono.just(new RedisRateLimiter.Response(false, Map.of())));

        MockServerWebExchange exchange = exchange("auth-login", "/api/auth/login");
        filter("auth").filter(exchange, ignored -> Mono.empty()).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(exchange.getResponse().getHeaders().getFirst("Retry-After")).isEqualTo("1");
        assertThat(meters.counter("rate_limit_rejected_total", "route", "auth-login", "tier", "auth").count())
                .isEqualTo(1);
    }

    @Test
    void sharesLimitAcrossTwoGatewayInstances() {
        when(keys.resolve(org.mockito.ArgumentMatchers.any())).thenReturn(Mono.just("ip:203.0.113.9"));
        when(limiter.isAllowed(anyString(), anyString()))
                .thenReturn(Mono.just(new RedisRateLimiter.Response(true, Map.of())))
                .thenReturn(Mono.just(new RedisRateLimiter.Response(false, Map.of())));
        ClientRateLimitGatewayFilterFactory first = new ClientRateLimitGatewayFilterFactory(
                limiter, keys, properties, meters);
        ClientRateLimitGatewayFilterFactory second = new ClientRateLimitGatewayFilterFactory(
                limiter, keys, properties, meters);

        MockServerWebExchange firstExchange = exchange("auth-login", "/api/auth/login");
        MockServerWebExchange secondExchange = exchange("auth-login", "/api/auth/login");
        first.apply(config("auth")).filter(firstExchange, ignored -> Mono.empty()).block();
        second.apply(config("auth")).filter(secondExchange, ignored -> Mono.empty()).block();

        assertThat(firstExchange.getResponse().getStatusCode()).isNotEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(secondExchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    @Test
    void keysByUserWhenAuthenticated() {
        ClientKeyResolver resolver = new ClientKeyResolver(properties);
        Jwt jwt = Jwt.withTokenValue("test").header("alg", "none").subject("learner-42")
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60)).build();
        ServerWebExchange exchange = exchange("test", "/").mutate()
                .principal(Mono.just(new JwtAuthenticationToken(jwt))).build();

        assertThat(resolver.resolve(exchange).block()).isEqualTo("user:learner-42");
    }

    @Test
    void keysByIpWhenAnonymous() {
        ClientKeyResolver resolver = new ClientKeyResolver(properties);

        assertThat(resolver.resolve(exchange("test", "/").mutate()
                        .request(MockServerHttpRequest.get("/").remoteAddress(
                                new java.net.InetSocketAddress("203.0.113.7", 1234)))
                        .build()))
                .block()).isEqualTo("ip:203.0.113.7");
    }

    @Test
    void ignoresForwardedForFromUntrustedProxy() {
        ClientKeyResolver resolver = new ClientKeyResolver(properties);
        ServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/")
                .header("X-Forwarded-For", "198.51.100.10")
                .remoteAddress(new java.net.InetSocketAddress("203.0.113.7", 1234)));

        assertThat(resolver.resolve(exchange).block()).isEqualTo("ip:203.0.113.7");
    }

    @Test
    void letsRequestsThroughWhenRedisIsDown() {
        when(keys.resolve(org.mockito.ArgumentMatchers.any())).thenReturn(Mono.just("ip:203.0.113.9"));
        // RedisRateLimiter returns an allowed response when its Redis script errors.
        when(limiter.isAllowed(anyString(), anyString())).thenReturn(Mono.just(new RedisRateLimiter.Response(true, Map.of())));
        MockServerWebExchange exchange = exchange("read-api", "/api/learning/topics");

        filter("default").filter(exchange, ignored -> Mono.empty()).block();

        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }

    private GatewayFilter filter(String tier) {
        ClientRateLimitGatewayFilterFactory factory = new ClientRateLimitGatewayFilterFactory(
                limiter, keys, properties, meters);
        return factory.apply(config(tier));
    }

    private ClientRateLimitGatewayFilterFactory.Config config(String tier) {
        ClientRateLimitGatewayFilterFactory.Config config = new ClientRateLimitGatewayFilterFactory.Config();
        config.setTier(tier);
        return config;
    }

    private MockServerWebExchange exchange(String routeId, String path) {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get(path));
        exchange.getAttributes().put(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR,
                Route.async().id(routeId).uri("http://localhost").predicate(ignored -> true).build());
        return exchange;
    }
}
