package com.group01.game.application.port;

import com.group01.game.domain.aggregate.GameAnswer;
import com.group01.game.domain.aggregate.GameSession;

import java.time.Instant;
import java.util.UUID;

public interface GameMatchEventWriter {
    void recordAnswer(UUID matchId, UUID matchPlayerId, GameAnswer answer, GameSession session, Instant occurredAt);
}
