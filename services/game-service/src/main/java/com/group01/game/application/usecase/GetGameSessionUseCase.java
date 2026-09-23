package com.group01.game.application.usecase;

import com.group01.game.application.result.GameSessionResult;
import com.group01.game.domain.exception.GameSessionNotFoundException;
import com.group01.game.domain.repository.GameSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class GetGameSessionUseCase {
    private final GameSessionRepository sessionRepository;

    public GetGameSessionUseCase(GameSessionRepository sessionRepository) { this.sessionRepository = sessionRepository; }

    @Transactional(readOnly = true)
    public GameSessionResult execute(UUID sessionId, UUID userId) {
        return sessionRepository.findOwned(sessionId, userId)
                .map(session -> GameSessionResult.from(session, false))
                .orElseThrow(() -> new GameSessionNotFoundException(sessionId));
    }
}
