package com.group01.game.application.port;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface WebSocketTicketStore {
    Ticket issue(UUID roomId, UUID userId);
    Optional<Ticket> consume(String token);

    record Ticket(String token, UUID roomId, UUID userId, Instant expiresAt) {}
}
