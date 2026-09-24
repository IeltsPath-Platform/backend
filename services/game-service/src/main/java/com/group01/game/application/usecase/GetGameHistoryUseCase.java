package com.group01.game.application.usecase;

import com.group01.game.application.result.GameHistoryResult;
import com.group01.game.application.result.GameSessionResult;
import com.group01.game.domain.repository.GameSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class GetGameHistoryUseCase {
    private final GameSessionRepository sessionRepository;

    public GetGameHistoryUseCase(GameSessionRepository sessionRepository) { this.sessionRepository = sessionRepository; }

    @Transactional(readOnly = true)
    public GameHistoryResult execute(UUID userId, int page, int size) {
        if (page < 0) throw new IllegalArgumentException("page must not be negative");
        if (size < 1 || size > 50) throw new IllegalArgumentException("size must be between 1 and 50");
        var result = sessionRepository.findHistory(userId, page, size);
        return new GameHistoryResult(result.sessions().stream().map(GameSessionResult::from).toList(),
                result.page(), result.size(), result.totalElements(), result.totalPages());
    }
}
