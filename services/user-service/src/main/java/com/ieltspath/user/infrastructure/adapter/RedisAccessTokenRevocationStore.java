package com.ieltspath.user.infrastructure.adapter;

import com.ieltspath.user.application.port.AccessTokenRevocationStore;
import com.ieltspath.user.config.AuthTokenProperties;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisAccessTokenRevocationStore implements AccessTokenRevocationStore {
    private final StringRedisTemplate redis;
    private final AuthTokenProperties properties;
    private final MeterRegistry meters;

    @Override
    public void revokeSession(UUID sessionId) {
        afterCommit(() -> write("auth:revoked:sid:" + sessionId, "1", ttl()));
    }

    @Override
    public void revokeUser(UUID userId, Instant revokedAt) {
        afterCommit(() -> write("auth:revoked:user:" + userId, Long.toString(revokedAt.getEpochSecond()), ttl()));
    }

    private Duration ttl() {
        return Duration.ofSeconds(properties.accessTokenMaxAgeSeconds() + 60L);
    }

    private void afterCommit(Runnable operation) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { operation.run(); }
            });
        } else operation.run();
    }

    private void write(String key, String value, Duration ttl) {
        try {
            redis.opsForValue().set(key, value, ttl);
        } catch (RuntimeException exception) {
            meters.counter("auth.revocation.write.errors").increment();
            log.warn("Không ghi được trạng thái thu hồi access token");
        }
    }
}
