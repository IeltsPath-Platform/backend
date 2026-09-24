package com.group01.game.api.dto.response;

import com.group01.game.application.result.GameTicketResult;

import java.time.Instant;
import java.util.List;

public record GameTicketResponse(String ticket, Instant expiresAt, List<String> subprotocols) {
    public GameTicketResponse {
        subprotocols = List.copyOf(subprotocols);
    }

    public static GameTicketResponse from(GameTicketResult result) {
        return new GameTicketResponse(result.ticket(), result.expiresAt(), result.subprotocols());
    }
}
