package com.group01.game.application.usecase;

import com.group01.game.application.result.GameSessionResult;
import com.group01.game.application.port.OutboxWriter;
import com.group01.game.domain.exception.GameSessionNotFoundException;
import com.group01.game.domain.repository.GameSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class AbandonGameSessionUseCase {
    private final GameSessionRepository sessionRepository;
    private final OutboxWriter outboxWriter;

    public AbandonGameSessionUseCase(GameSessionRepository sessionRepository, OutboxWriter outboxWriter) {
        this.sessionRepository = sessionRepository;
        this.outboxWriter = outboxWriter;
    }

    @Transactional
    public GameSessionResult execute(UUID sessionId, UUID userId) {
        var session = sessionRepository.findOwned(sessionId, userId)
                .orElseThrow(() -> new GameSessionNotFoundException(sessionId));
        session.abandon(Instant.now());
        var saved = sessionRepository.save(session);
        outboxWriter.append("GameSession", sessionId.toString(), "GameSessionAbandoned",
                java.util.Map.of("sessionId", sessionId.toString(), "userId", userId.toString()));
        return GameSessionResult.from(saved, false);
    }
}
