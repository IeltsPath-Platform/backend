package com.ieltspath.apigateway.security;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

@Component
@RequiredArgsConstructor
public class RevocationChecker {
    private final ReactiveStringRedisTemplate redis;
    private final MeterRegistry meters;

    public Mono<Boolean> isRevoked(Jwt jwt) {
        String sessionId = jwt.getClaimAsString("sid");
        String tokenId = jwt.getId();
        String userId = jwt.getSubject();
        if (sessionId == null || tokenId == null || jwt.getIssuedAt() == null || userId == null) {
            return Mono.just(false);
        }
        String sessionKey = "auth:revoked:sid:" + sessionId;
        String tokenKey = "auth:revoked:jti:" + tokenId;
        String userKey = "auth:revoked:user:" + userId;
        return redis.opsForValue().multiGet(List.of(sessionKey, tokenKey, userKey))
                .timeout(Duration.ofMillis(100))
                .map(values -> values != null && values.size() == 3
                        && (values.get(0) != null || values.get(1) != null
                        || (values.get(2) != null && jwt.getIssuedAt().getEpochSecond()
                        < Long.parseLong(values.get(2)))))
                .doOnError(error -> meters.counter("gateway.auth.revocation.errors").increment());
    }
}
