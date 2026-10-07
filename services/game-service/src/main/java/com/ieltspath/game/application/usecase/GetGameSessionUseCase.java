package com.ieltspath.game.application.usecase;

import com.ieltspath.game.application.result.GameSessionResult;
import com.ieltspath.game.domain.exception.GameSessionNotFoundException;
import com.ieltspath.game.domain.repository.GameSessionRepository;
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
                .map(GameSessionResult::from)
                .orElseThrow(() -> new GameSessionNotFoundException(sessionId));
    }
}
