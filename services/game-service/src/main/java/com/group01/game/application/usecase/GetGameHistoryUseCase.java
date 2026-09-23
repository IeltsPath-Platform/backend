package com.group01.game.application.usecase;

import com.group01.game.application.result.GameSessionResult;
import com.group01.game.domain.repository.GameSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class GetGameHistoryUseCase {
    private final GameSessionRepository sessionRepository;

    public GetGameHistoryUseCase(GameSessionRepository sessionRepository) { this.sessionRepository = sessionRepository; }

    @Transactional(readOnly = true)
    public HistoryResult execute(UUID userId, int page, int size) {
        if (page < 0) throw new IllegalArgumentException("page must not be negative");
        if (size < 1 || size > 50) throw new IllegalArgumentException("size must be between 1 and 50");
        var result = sessionRepository.findHistory(userId, page, size);
        return new HistoryResult(result.sessions().stream().map(session -> GameSessionResult.from(session, false)).toList(),
                result.page(), result.size(), result.totalElements(), result.totalPages());
    }

    public record HistoryResult(List<GameSessionResult> content, int page, int size,
                                long totalElements, long totalPages) {
        public HistoryResult { content = List.copyOf(content); }
    }
}
