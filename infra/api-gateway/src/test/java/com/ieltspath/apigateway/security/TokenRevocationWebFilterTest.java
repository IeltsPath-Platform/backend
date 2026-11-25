package com.ieltspath.apigateway.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TokenRevocationWebFilterTest {
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void doesNotForwardARevokedSession() {
        RevocationChecker checker = mock(RevocationChecker.class);
        when(checker.isRevoked(org.mockito.ArgumentMatchers.any())).thenReturn(Mono.just(true));
        var filter = new TokenRevocationWebFilter(checker, new RevocationProperties(false), mapper,
                new SimpleMeterRegistry());
        var exchange = exchange();
        AtomicInteger calls = new AtomicInteger();

        filter.filter(exchange, ignored -> { calls.incrementAndGet(); return Mono.empty(); })
                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication())).block();

        assertThat(calls.get()).isZero();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(exchange.getResponse().getBodyAsString().block()).contains("SESSION_REVOKED")
                .contains("Phiên đăng nhập đã hết hiệu lực. Vui lòng đăng nhập lại.");
    }

    @Test
    void forwardsOnceWhenRevocationStoreFailsInFailOpenMode() {
        RevocationChecker checker = mock(RevocationChecker.class);
        when(checker.isRevoked(org.mockito.ArgumentMatchers.any())).thenReturn(Mono.error(new IllegalStateException()));
        var filter = new TokenRevocationWebFilter(checker, new RevocationProperties(false), mapper,
                new SimpleMeterRegistry());
        var exchange = exchange();
        AtomicInteger calls = new AtomicInteger();

        filter.filter(exchange, ignored -> { calls.incrementAndGet(); return Mono.empty(); })
                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication())).block();

        assertThat(calls.get()).isEqualTo(1);
        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }

    @Test
    void returnsUnavailableWhenRevocationStoreFailsInFailClosedMode() {
        RevocationChecker checker = mock(RevocationChecker.class);
        when(checker.isRevoked(org.mockito.ArgumentMatchers.any())).thenReturn(Mono.error(new IllegalStateException()));
        var filter = new TokenRevocationWebFilter(checker, new RevocationProperties(true), mapper,
                new SimpleMeterRegistry());
        var exchange = exchange();
        AtomicInteger calls = new AtomicInteger();

        filter.filter(exchange, ignored -> { calls.incrementAndGet(); return Mono.empty(); })
                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication())).block();

        assertThat(calls.get()).isZero();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(exchange.getResponse().getBodyAsString().block()).contains("AUTH_UNAVAILABLE")
                .contains("Hệ thống tạm thời không xác minh được phiên đăng nhập. Vui lòng thử lại sau.");
    }

    @Test
    void forwardsOnceWhenTokenIsNotRevoked() {
        RevocationChecker checker = mock(RevocationChecker.class);
        when(checker.isRevoked(org.mockito.ArgumentMatchers.any())).thenReturn(Mono.just(false));
        var filter = new TokenRevocationWebFilter(checker, new RevocationProperties(false), mapper,
                new SimpleMeterRegistry());
        AtomicInteger calls = new AtomicInteger();

        filter.filter(exchange(), ignored -> { calls.incrementAndGet(); return Mono.empty(); })
                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication())).block();

        assertThat(calls.get()).isEqualTo(1);
    }

    @Test
    void forwardsOnceWhenNoAuthenticationIsPresent() {
        var filter = new TokenRevocationWebFilter(mock(RevocationChecker.class), new RevocationProperties(false), mapper,
                new SimpleMeterRegistry());
        AtomicInteger calls = new AtomicInteger();

        filter.filter(exchange(), ignored -> { calls.incrementAndGet(); return Mono.empty(); }).block();

        assertThat(calls.get()).isEqualTo(1);
    }

    @Test
    void propagatesDownstreamErrorsWithoutReturningAuthenticationUnavailable() {
        RevocationChecker checker = mock(RevocationChecker.class);
        when(checker.isRevoked(org.mockito.ArgumentMatchers.any())).thenReturn(Mono.just(false));
        var filter = new TokenRevocationWebFilter(checker, new RevocationProperties(true), mapper,
                new SimpleMeterRegistry());
        AtomicInteger calls = new AtomicInteger();
        var exchange = exchange();

        assertThatThrownBy(() -> filter.filter(exchange, ignored -> {
            calls.incrementAndGet();
            return Mono.error(new IllegalStateException("downstream failed"));
        }).contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication())).block())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("downstream failed");

        assertThat(calls.get()).isEqualTo(1);
        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }

    private MockServerWebExchange exchange() {
        return MockServerWebExchange.from(MockServerHttpRequest.get("/api/users/me").build());
    }

    private JwtAuthenticationToken authentication() {
        Instant now = Instant.now();
        Jwt jwt = Jwt.withTokenValue("access").header("alg", "HS256").subject(UUID.randomUUID().toString())
                .jti(UUID.randomUUID().toString()).issuedAt(now).expiresAt(now.plusSeconds(300))
                .claim("sid", UUID.randomUUID().toString()).build();
        return new JwtAuthenticationToken(jwt);
    }
}
