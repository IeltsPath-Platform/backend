package com.ieltspath.game.application.port;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface WebSocketTicketStore {
    Ticket issue(UUID roomId, UUID userId, Set<String> roles);
    Optional<Ticket> consume(String token);

    record Ticket(String token, UUID roomId, UUID userId, Instant expiresAt, Set<String> roles) {
        public Ticket {
            roles = roles == null ? Set.of() : Set.copyOf(roles);
        }
    }
}
