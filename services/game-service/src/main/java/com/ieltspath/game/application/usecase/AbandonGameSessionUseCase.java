package com.ieltspath.game.application.usecase;

import com.ieltspath.game.application.result.GameSessionResult;
import com.ieltspath.game.domain.exception.GameSessionNotFoundException;
import com.ieltspath.game.domain.repository.GameSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class AbandonGameSessionUseCase {
    private final GameSessionRepository sessionRepository;

    public AbandonGameSessionUseCase(GameSessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    @Transactional
    public GameSessionResult execute(UUID sessionId, UUID userId) {
        var session = sessionRepository.findOwned(sessionId, userId)
                .orElseThrow(() -> new GameSessionNotFoundException(sessionId));
        session.abandon(Instant.now());
        var saved = sessionRepository.save(session);
        return GameSessionResult.from(saved);
    }
}
