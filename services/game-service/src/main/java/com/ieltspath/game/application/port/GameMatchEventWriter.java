package com.ieltspath.game.application.port;

import com.ieltspath.game.domain.aggregate.GameAnswer;
import com.ieltspath.game.domain.aggregate.GameSession;

import java.time.Instant;
import java.util.UUID;

public interface GameMatchEventWriter {
    void recordAnswer(UUID matchId, UUID matchPlayerId, GameAnswer answer, GameSession session, Instant occurredAt);
}
