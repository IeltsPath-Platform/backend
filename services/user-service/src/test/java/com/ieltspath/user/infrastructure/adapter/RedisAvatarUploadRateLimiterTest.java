package com.ieltspath.user.infrastructure.adapter;

import com.ieltspath.user.application.exception.AvatarUploadRateLimitException;
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
class RedisAvatarUploadRateLimiterTest {
    @Container
    private static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    private LettuceConnectionFactory connectionFactory;
    private RedisAvatarUploadRateLimiter limiter;

    @BeforeEach
    void setUp() {
        connectionFactory = new LettuceConnectionFactory(REDIS.getHost(), REDIS.getMappedPort(6379));
        connectionFactory.afterPropertiesSet();
        StringRedisTemplate redis = new StringRedisTemplate(connectionFactory);
        redis.afterPropertiesSet();
        limiter = new RedisAvatarUploadRateLimiter(redis);
    }

    @AfterEach
    void tearDown() {
        connectionFactory.destroy();
    }

    @Test
    void preservesTenUploadsPerHourPerUserLimit() {
        UUID userId = UUID.randomUUID();
        for (int attempt = 0; attempt < 10; attempt++) limiter.checkAndAcquire(userId);

        assertThatThrownBy(() -> limiter.checkAndAcquire(userId)).isInstanceOf(AvatarUploadRateLimitException.class);
    }

    @Test
    void keepsUsersOnSeparateCounters() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        for (int attempt = 0; attempt < 10; attempt++) limiter.checkAndAcquire(first);

        limiter.checkAndAcquire(second);
    }
}
