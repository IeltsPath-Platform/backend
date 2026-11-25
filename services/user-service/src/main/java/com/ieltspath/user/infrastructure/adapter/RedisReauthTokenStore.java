package com.ieltspath.user.infrastructure.adapter;

import com.ieltspath.user.application.port.ReauthTokenStore;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

@Component
public class RedisReauthTokenStore implements ReauthTokenStore {
    private static final Duration TTL = Duration.ofSeconds(300);
    private final StringRedisTemplate redis;
    private final SecureRandom random = new SecureRandom();

    public RedisReauthTokenStore(StringRedisTemplate redis) { this.redis = redis; }

    @Override
    public String issue(UUID userId, String method, UUID sessionId) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        redis.opsForValue().set(key(token), userId + "|" + method + "|" + sessionId, TTL);
        return token;
    }

    @Override
    public Optional<Grant> consume(String token) {
        if (token == null || token.isBlank()) return Optional.empty();
        String value = redis.opsForValue().getAndDelete(key(token));
        if (value == null) return Optional.empty();
        String[] parts = value.split("\\|", -1);
        if (parts.length != 3) return Optional.empty();
        try {
            return Optional.of(new Grant(UUID.fromString(parts[0]), parts[1], UUID.fromString(parts[2])));
        } catch (IllegalArgumentException malformed) {
            return Optional.empty();
        }
    }

    private String key(String token) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return "auth:reauth:" + HexFormat.of().formatHex(hash);
        } catch (java.security.NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("Không thể tạo mã xác thực.", impossible);
        }
    }
}
