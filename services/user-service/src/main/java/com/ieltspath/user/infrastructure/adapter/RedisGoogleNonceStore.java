package com.ieltspath.user.infrastructure.adapter;

import com.ieltspath.user.application.port.GoogleNonceStore;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;

@Component
public class RedisGoogleNonceStore implements GoogleNonceStore {
    private static final Duration TTL = Duration.ofSeconds(300);
    private final StringRedisTemplate redis;
    private final SecureRandom random = new SecureRandom();

    public RedisGoogleNonceStore(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public String issue() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String nonce = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        redis.opsForValue().set(key(nonce), "1", TTL);
        return nonce;
    }

    @Override
    public boolean consume(String nonce) {
        if (nonce == null || nonce.isBlank()) return false;
        return "1".equals(redis.opsForValue().getAndDelete(key(nonce)));
    }

    private String key(String nonce) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(nonce.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return "auth:google:nonce:" + HexFormat.of().formatHex(digest);
        } catch (java.security.NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("Không thể tạo mã xác thực.", impossible);
        }
    }
}
