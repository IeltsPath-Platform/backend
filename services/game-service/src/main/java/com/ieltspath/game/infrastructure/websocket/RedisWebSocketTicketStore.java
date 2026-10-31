package com.ieltspath.game.infrastructure.websocket;

import com.ieltspath.game.application.port.WebSocketTicketStore;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Keeps WebSocket tickets in Redis so that a ticket issued by one instance can be redeemed at another. Redeeming
 * deletes the key atomically, so a ticket works once. Redis expires the key after the ticket TTL.
 */
@Component
public class RedisWebSocketTicketStore implements WebSocketTicketStore {
    private static final String KEY_PREFIX = "game:ws-ticket:";
    private static final char SEPARATOR = '|';

    private final StringRedisTemplate redis;
    private final Duration ttl;
    private final SecureRandom secureRandom = new SecureRandom();

    public RedisWebSocketTicketStore(StringRedisTemplate redis,
                                     @Value("${game.websocket.ticket-ttl:PT30S}") Duration ttl) {
        this.redis = redis;
        this.ttl = ttl;
    }

    @Override
    public Ticket issue(UUID roomId, UUID userId) {
        byte[] tokenBytes = new byte[32];
        secureRandom.nextBytes(tokenBytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
        Instant expiresAt = Instant.now().plus(ttl);
        redis.opsForValue().set(KEY_PREFIX + token, roomId + String.valueOf(SEPARATOR) + userId, ttl);
        return new Ticket(token, roomId, userId, expiresAt);
    }

    @Override
    public Optional<Ticket> consume(String token) {
        if (token == null || token.isBlank()) return Optional.empty();
        String stored = redis.opsForValue().getAndDelete(KEY_PREFIX + token);
        if (stored == null) return Optional.empty();
        int split = stored.indexOf(SEPARATOR);
        UUID roomId = UUID.fromString(stored.substring(0, split));
        UUID userId = UUID.fromString(stored.substring(split + 1));
        return Optional.of(new Ticket(token, roomId, userId, Instant.now().plus(ttl)));
    }
}
