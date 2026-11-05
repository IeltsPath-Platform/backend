package com.ieltspath.community.infrastructure.adapter;

import com.ieltspath.community.domain.exception.CommunityRateLimitExceededException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
class RedisMediaRateLimiterTest {
    @Container
    private static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    private LettuceConnectionFactory connectionFactory;
    private RedisMediaRateLimiter limiter;

    @BeforeEach
    void setUp() {
        connectionFactory = new LettuceConnectionFactory(REDIS.getHost(), REDIS.getMappedPort(6379));
        connectionFactory.afterPropertiesSet();
        StringRedisTemplate redis = new StringRedisTemplate(connectionFactory);
        redis.afterPropertiesSet();
        limiter = new RedisMediaRateLimiter(redis);
    }

    @AfterEach
    void tearDown() {
        connectionFactory.destroy();
    }

    @Test
    void preservesThirtyUploadsPerHourPerUserLimit() {
        UUID userId = UUID.randomUUID();
        for (int attempt = 0; attempt < 30; attempt++) limiter.checkAndAcquire(userId);

        assertThatThrownBy(() -> limiter.checkAndAcquire(userId)).isInstanceOf(CommunityRateLimitExceededException.class);
    }

    @Test
    void allowsNullUserId() {
        limiter.checkAndAcquire(null);
    }

    @Test
    void keepsUsersOnSeparateCounters() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        for (int attempt = 0; attempt < 30; attempt++) limiter.checkAndAcquire(first);

        limiter.checkAndAcquire(second);
    }
}
