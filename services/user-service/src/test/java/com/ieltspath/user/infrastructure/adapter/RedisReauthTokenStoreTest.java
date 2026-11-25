package com.ieltspath.user.infrastructure.adapter;

import com.ieltspath.user.application.port.ReauthTokenStore;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
class RedisReauthTokenStoreTest {
    @Container private static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);
    private static LettuceConnectionFactory factory;
    private static StringRedisTemplate redis;
    private static RedisReauthTokenStore store;

    @BeforeAll
    static void connect() {
        factory = new LettuceConnectionFactory(REDIS.getHost(), REDIS.getMappedPort(6379));
        factory.afterPropertiesSet();
        redis = new StringRedisTemplate(factory);
        redis.afterPropertiesSet();
        store = new RedisReauthTokenStore(redis);
    }

    @AfterAll
    static void close() { if (factory != null) factory.destroy(); }

    @Test
    void storesOnlyTheTokenHashAndConsumesTheUserMethodAndSessionGrantOnce() {
        var userId = java.util.UUID.randomUUID();
        var sessionId = java.util.UUID.randomUUID();
        String token = store.issue(userId, "GOOGLE", sessionId);
        var keys = redis.keys("auth:reauth:*");

        assertThat(token).hasSize(43);
        assertThat(keys).hasSize(1);
        assertThat(keys.iterator().next()).matches("auth:reauth:[0-9a-f]{64}");
        assertThat(redis.getExpire(keys.iterator().next())).isBetween(1L, 300L);
        assertThat(store.consume(token)).contains(new ReauthTokenStore.Grant(userId, "GOOGLE", sessionId));
        assertThat(store.consume(token)).isEmpty();
    }
}
