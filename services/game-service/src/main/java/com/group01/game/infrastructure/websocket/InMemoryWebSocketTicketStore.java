package com.group01.game.infrastructure.websocket;

import com.group01.game.application.port.WebSocketTicketStore;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryWebSocketTicketStore implements WebSocketTicketStore {
    private static final long TICKET_TTL_SECONDS = 30;
    private final SecureRandom secureRandom = new SecureRandom();
    private final ConcurrentHashMap<String, Ticket> tickets = new ConcurrentHashMap<>();

    @Override
    public Ticket issue(UUID roomId, UUID userId) {
        Instant now = Instant.now();
        tickets.entrySet().removeIf(entry -> !entry.getValue().expiresAt().isAfter(now));
        byte[] tokenBytes = new byte[32];
        secureRandom.nextBytes(tokenBytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
        Ticket ticket = new Ticket(token, roomId, userId, now.plusSeconds(TICKET_TTL_SECONDS));
        tickets.put(token, ticket);
        return ticket;
    }

    @Override
    public Optional<Ticket> consume(String token) {
        if (token == null || token.isBlank()) return Optional.empty();
        Ticket ticket = tickets.remove(token);
        return ticket != null && ticket.expiresAt().isAfter(Instant.now()) ? Optional.of(ticket) : Optional.empty();
    }
}
