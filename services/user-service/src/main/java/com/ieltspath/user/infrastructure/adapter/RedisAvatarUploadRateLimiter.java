package com.ieltspath.user.infrastructure.adapter;

import com.ieltspath.user.application.exception.AvatarUploadRateLimitException;
import com.ieltspath.user.application.port.AvatarUploadRateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Component
public class RedisAvatarUploadRateLimiter implements AvatarUploadRateLimiter {
    private static final Logger log = LoggerFactory.getLogger(RedisAvatarUploadRateLimiter.class);
    private static final int MAX_PER_HOUR = 10;
    private static final Duration WINDOW = Duration.ofHours(1);
    private static final DefaultRedisScript<Long> ACQUIRE = new DefaultRedisScript<>("""
            local now = tonumber(ARGV[1])
            redis.call('ZREMRANGEBYSCORE', KEYS[1], '-inf', now - tonumber(ARGV[2]))
            if redis.call('ZCARD', KEYS[1]) >= tonumber(ARGV[3]) then return 0 end
            redis.call('ZADD', KEYS[1], now, ARGV[4])
            redis.call('PEXPIRE', KEYS[1], ARGV[2])
            return 1
            """, Long.class);

    private final StringRedisTemplate redis;

    public RedisAvatarUploadRateLimiter(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public void checkAndAcquire(UUID userId) {
        if (userId == null) return;
        try {
            Long acquired = redis.execute(ACQUIRE, List.of("avatar-upload:" + userId),
                    Long.toString(System.currentTimeMillis()), Long.toString(WINDOW.toMillis()),
                    Integer.toString(MAX_PER_HOUR), UUID.randomUUID().toString());
            if (Long.valueOf(0).equals(acquired)) throw new AvatarUploadRateLimitException();
        } catch (AvatarUploadRateLimitException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            log.warn("Avatar upload rate-limit backend unavailable; allowing upload", exception);
        }
    }
}
