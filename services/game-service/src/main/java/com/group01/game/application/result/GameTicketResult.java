package com.group01.game.application.result;

import java.time.Instant;
import java.util.List;

public record GameTicketResult(String ticket, Instant expiresAt, List<String> subprotocols) {
    public GameTicketResult {
        subprotocols = List.copyOf(subprotocols);
    }
}
