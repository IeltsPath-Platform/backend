package com.ieltspath.user.infrastructure.adapter;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
class RedisGoogleNonceStoreTest {
    @Container
    private static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);
    private static LettuceConnectionFactory connectionFactory;
    private static StringRedisTemplate redis;
    private static RedisGoogleNonceStore store;

    @BeforeAll
    static void connect() {
        connectionFactory = new LettuceConnectionFactory(REDIS.getHost(), REDIS.getMappedPort(6379));
        connectionFactory.afterPropertiesSet();
        redis = new StringRedisTemplate(connectionFactory);
        redis.afterPropertiesSet();
        store = new RedisGoogleNonceStore(redis);
    }

    @AfterAll
    static void close() {
        if (connectionFactory != null) connectionFactory.destroy();
    }

    @Test
    void issuesAHashedNonceWithFiveMinuteExpiryAndConsumesItOnlyOnce() throws Exception {
        String nonce = store.issue();
        String key = key(nonce);

        assertThat(nonce).hasSize(43);
        assertThat(nonce).matches("[A-Za-z0-9_-]{43}");
        assertThat(redis.keys("auth:google:nonce:*")).containsExactly(key);
        assertThat(key).matches("auth:google:nonce:[0-9a-f]{64}");
        assertThat(redis.getExpire(key)).isBetween(1L, 300L);
        assertThat(store.consume(nonce)).isTrue();
        assertThat(store.consume(nonce)).isFalse();
    }

    @Test
    void consumesTheNonceAtomicallyWhenRequestsRace() throws Exception {
        String nonce = store.issue();
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> store.consume(nonce));
            var second = executor.submit(() -> store.consume(nonce));
            assertThat((first.get() ? 1 : 0) + (second.get() ? 1 : 0)).isEqualTo(1);
        }
    }

    private String key(String nonce) throws Exception {
        byte[] hash = MessageDigest.getInstance("SHA-256")
                .digest(nonce.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        return "auth:google:nonce:" + HexFormat.of().formatHex(hash);
    }
}
