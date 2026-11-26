package com.ieltspath.game.infrastructure.websocket;

import com.ieltspath.game.application.port.WebSocketTicketStore;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.Set;
import java.util.Arrays;
import java.util.stream.Collectors;
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
    public Ticket issue(UUID roomId, UUID userId, Set<String> roles) {
        byte[] tokenBytes = new byte[32];
        secureRandom.nextBytes(tokenBytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
        Instant expiresAt = Instant.now().plus(ttl);
        String stored = roomId + String.valueOf(SEPARATOR) + userId + SEPARATOR
                + String.join(",", roles.stream().sorted().toList());
        redis.opsForValue().set(KEY_PREFIX + token, stored, ttl);
        return new Ticket(token, roomId, userId, expiresAt, roles);
    }

    @Override
    public Optional<Ticket> consume(String token) {
        if (token == null || token.isBlank()) return Optional.empty();
        String stored = redis.opsForValue().getAndDelete(KEY_PREFIX + token);
        if (stored == null) return Optional.empty();
        String[] parts = stored.split("\\|", -1);
        if (parts.length != 3) return Optional.empty();
        UUID roomId = UUID.fromString(parts[0]);
        UUID userId = UUID.fromString(parts[1]);
        Set<String> roles = parts[2].isBlank() ? Set.of() : Arrays.stream(parts[2].split(","))
                .collect(Collectors.toUnmodifiableSet());
        return Optional.of(new Ticket(token, roomId, userId, Instant.now().plus(ttl), roles));
    }
}
