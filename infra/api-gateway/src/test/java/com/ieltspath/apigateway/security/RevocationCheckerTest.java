package com.ieltspath.apigateway.security;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.springframework.security.oauth2.jwt.Jwt;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.Arrays;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RevocationCheckerTest {
    @SuppressWarnings("unchecked")
    private Fixture fixture(List<String> values) {
        var redis = mock(ReactiveStringRedisTemplate.class);
        var operations = mock(ReactiveValueOperations.class);
        when(redis.opsForValue()).thenReturn(operations);
        when(operations.multiGet(anyList())).thenReturn(Mono.just(values));
        return new Fixture(new RevocationChecker(redis, new SimpleMeterRegistry()));
    }

    @Test
    void rejectsSessionAndIndividualTokenRevocation() {
        assertThat(fixture(Arrays.asList("1", null, null)).checker.isRevoked(jwt(100)).block()).isTrue();
        assertThat(fixture(Arrays.asList(null, "1", null)).checker.isRevoked(jwt(100)).block()).isTrue();
    }

    @Test
    void rejectsOnlyTokensIssuedBeforeTheUserRevocationCutoff() {
        assertThat(fixture(Arrays.asList(null, null, "101")).checker.isRevoked(jwt(100)).block()).isTrue();
        assertThat(fixture(Arrays.asList(null, null, "100")).checker.isRevoked(jwt(100)).block()).isFalse();
        assertThat(fixture(Arrays.asList(null, null, "99")).checker.isRevoked(jwt(100)).block()).isFalse();
    }

    @Test
    void allowsLegacyTokensThatDoNotCarryAllRevocationClaims() {
        var redis = mock(ReactiveStringRedisTemplate.class);
        var operations = mock(ReactiveValueOperations.class);
        var checker = new RevocationChecker(redis, new SimpleMeterRegistry());
        Jwt legacy = Jwt.withTokenValue("legacy").header("alg", "HS256")
                .subject(UUID.randomUUID().toString()).expiresAt(Instant.now().plusSeconds(300)).build();

        assertThat(checker.isRevoked(legacy).block()).isFalse();
        org.mockito.Mockito.verifyNoInteractions(redis, operations);
    }

    private Jwt jwt(long issuedAt) {
        return Jwt.withTokenValue("access").header("alg", "HS256").subject(UUID.randomUUID().toString())
                .id(UUID.randomUUID().toString()).issuedAt(Instant.ofEpochSecond(issuedAt))
                .expiresAt(Instant.ofEpochSecond(issuedAt + 300)).claim("sid", UUID.randomUUID().toString()).build();
    }

    private record Fixture(RevocationChecker checker) { }
}
