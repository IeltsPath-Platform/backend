package com.ieltspath.user.infrastructure.adapter;

import com.ieltspath.user.application.exception.LoginLimiterUnavailableException;
import com.ieltspath.user.application.exception.LoginRateLimitedException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.RedisConnectionFailureException;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Testcontainers
class RedisLoginAttemptLimiterTest {
    @Container
    private static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    private LettuceConnectionFactory connectionFactory;
    private RedisLoginAttemptLimiter limiter;

    @BeforeEach
    void setUp() {
        connectionFactory = new LettuceConnectionFactory(REDIS.getHost(), REDIS.getMappedPort(6379));
        connectionFactory.afterPropertiesSet();
        StringRedisTemplate redis = new StringRedisTemplate(connectionFactory);
        redis.afterPropertiesSet();
        try (var connection = redis.getConnectionFactory().getConnection()) {
            connection.serverCommands().flushDb();
        }
        limiter = new RedisLoginAttemptLimiter(redis);
    }

    @AfterEach
    void tearDown() {
        connectionFactory.destroy();
    }

    @Test
    void blocksSixthFailureFromSamePair() {
        for (int attempt = 0; attempt < 5; attempt++) limiter.recordFailure("learner@example.com", "203.0.113.9");

        assertThatThrownBy(() -> limiter.checkAllowed("learner@example.com", "203.0.113.9"))
                .isInstanceOf(LoginRateLimitedException.class)
                .satisfies(exception -> assertThat(((LoginRateLimitedException) exception).retryAfter())
                        .isBetween(Duration.ofSeconds(1), Duration.ofMinutes(15)));
    }

    @Test
    void allowsSameEmailFromOtherIp() {
        for (int attempt = 0; attempt < 5; attempt++) limiter.recordFailure("learner@example.com", "203.0.113.9");

        limiter.checkAllowed("learner@example.com", "203.0.113.10");
    }

    @Test
    void opensShortWaitWindowAfterTwentyFailuresAcrossIps() {
        for (int attempt = 0; attempt < 21; attempt++) {
            limiter.checkAllowed("learner@example.com", "203.0.113." + (attempt + 1));
            limiter.recordFailure("learner@example.com", "203.0.113." + (attempt + 1));
        }

        assertThatThrownBy(() -> limiter.checkAllowed("learner@example.com", "203.0.113.99"))
                .isInstanceOf(LoginRateLimitedException.class)
                .satisfies(exception -> assertThat(((LoginRateLimitedException) exception).retryAfter().toSeconds())
                        .isBetween(1L, 8L));
    }

    @Test
    void successClearsPairCounter() {
        for (int attempt = 0; attempt < 5; attempt++) limiter.recordFailure("learner@example.com", "203.0.113.9");

        limiter.recordSuccess("learner@example.com", "203.0.113.9");

        limiter.checkAllowed("learner@example.com", "203.0.113.9");
    }

    @Test
    void normalizesEmailCase() {
        for (int attempt = 0; attempt < 5; attempt++) limiter.recordFailure("Learner@Example.com", "203.0.113.9");

        assertThatThrownBy(() -> limiter.checkAllowed("learner@example.com", "203.0.113.9"))
                .isInstanceOf(LoginRateLimitedException.class);
    }

    @Test
    void failsClosedWhenRedisIsDown() {
        StringRedisTemplate unavailableRedis = mock(StringRedisTemplate.class);
        when(unavailableRedis.getExpire(anyString()))
                .thenThrow(new RedisConnectionFailureException("Redis unavailable"));
        RedisLoginAttemptLimiter unavailableLimiter = new RedisLoginAttemptLimiter(unavailableRedis);

        assertThatThrownBy(() -> unavailableLimiter.checkAllowed("learner@example.com", UUID.randomUUID().toString()))
                .isInstanceOf(LoginLimiterUnavailableException.class);
    }
}
