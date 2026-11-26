package com.ieltspath.user.infrastructure.adapter;

import com.ieltspath.user.application.exception.LoginLimiterUnavailableException;
import com.ieltspath.user.application.exception.LoginRateLimitedException;
import com.ieltspath.user.application.port.LoginAttemptLimiter;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;

/** Shared, expiring failed-login counters. Redis failures deliberately fail closed for authentication. */
@Component
public class RedisLoginAttemptLimiter implements LoginAttemptLimiter {
    private static final Duration WINDOW = Duration.ofMinutes(15);
    private static final Duration IP_WINDOW = Duration.ofMinutes(1);
    private static final long PAIR_THRESHOLD = 5;
    private static final long EMAIL_WAIT_THRESHOLD = 20;
    private static final long IP_THRESHOLD = 20;
    private static final DefaultRedisScript<Long> RECORD_FAILURE = new DefaultRedisScript<>("""
            local pair = redis.call('INCR', KEYS[1])
            if pair == 1 then redis.call('EXPIRE', KEYS[1], ARGV[1]) end
            local email = redis.call('INCR', KEYS[2])
            if email == 1 then redis.call('EXPIRE', KEYS[2], ARGV[1]) end
            local ip = redis.call('INCR', KEYS[4])
            if ip == 1 then redis.call('EXPIRE', KEYS[4], ARGV[3]) end
            if email > tonumber(ARGV[2]) then
              local exponent = math.min(3, email - tonumber(ARGV[2]) - 1)
              local delay = math.min(8, 2 ^ exponent)
              redis.call('SET', KEYS[3], '1', 'EX', delay)
              return delay
            end
            if ip > tonumber(ARGV[4]) then return 1 end
            return 0
            """, Long.class);

    private final StringRedisTemplate redis;

    public RedisLoginAttemptLimiter(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public void checkAllowed(String normalizedEmail, String clientIp) {
        String hash = hash(normalize(normalizedEmail));
        try {
            long waitTtl = ttlSeconds("login:wait:" + hash);
            if (waitTtl > 0) throw new LoginRateLimitedException(Duration.ofSeconds(waitTtl));

            String pairKey = pairKey(hash, clientIp);
            String count = redis.opsForValue().get(pairKey);
            if (count != null && Long.parseLong(count) >= PAIR_THRESHOLD) {
                throw new LoginRateLimitedException(Duration.ofSeconds(ttlSeconds(pairKey)));
            }
            String ipKey = ipKey(clientIp);
            String ipCount = redis.opsForValue().get(ipKey);
            if (ipCount != null && Long.parseLong(ipCount) >= IP_THRESHOLD) {
                throw new LoginRateLimitedException(Duration.ofSeconds(ttlSeconds(ipKey)));
            }
        } catch (LoginRateLimitedException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw unavailable(exception);
        }
    }

    @Override
    public void recordFailure(String normalizedEmail, String clientIp) {
        String hash = hash(normalize(normalizedEmail));
        try {
            redis.execute(RECORD_FAILURE,
                    List.of(pairKey(hash, clientIp), "login:email:" + hash, "login:wait:" + hash, ipKey(clientIp)),
                    Long.toString(WINDOW.toSeconds()), Long.toString(EMAIL_WAIT_THRESHOLD),
                    Long.toString(IP_WINDOW.toSeconds()), Long.toString(IP_THRESHOLD));
        } catch (RuntimeException exception) {
            throw unavailable(exception);
        }
    }

    @Override
    public void recordSuccess(String normalizedEmail, String clientIp) {
        String hash = hash(normalize(normalizedEmail));
        try {
            redis.delete(pairKey(hash, clientIp));
        } catch (RuntimeException exception) {
            throw unavailable(exception);
        }
    }

    private long ttlSeconds(String key) {
        Long ttl = redis.getExpire(key);
        return ttl == null ? 0 : Math.max(0, ttl);
    }

    private String pairKey(String emailHash, String clientIp) {
        return "login:pair:" + emailHash + ":" + (clientIp == null || clientIp.isBlank() ? "unknown" : clientIp.trim());
    }

    private String ipKey(String clientIp) {
        return "login:ip:" + hash(clientIp == null || clientIp.isBlank() ? "unknown" : clientIp.trim());
    }

    private String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private String hash(String email) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(email.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private LoginLimiterUnavailableException unavailable(RuntimeException exception) {
        return new LoginLimiterUnavailableException(exception);
    }
}
