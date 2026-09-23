package com.group01.game.domain.repository;

import com.group01.game.domain.aggregate.GameAnswer;
import com.group01.game.domain.aggregate.GameSession;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GameSessionRepository {
    GameSession save(GameSession session);
    Optional<GameSession> findOwned(UUID id, UUID userId);
    Optional<GameSession> findByMatchPlayerId(UUID matchPlayerId);
    SessionPage findHistory(UUID userId, int page, int size);
    Optional<GameAnswer> findAnswer(UUID sessionId, int itemSequence);
    List<GameAnswer> findAnswers(UUID sessionId);
    GameAnswer saveAnswer(GameAnswer answer);

    record SessionPage(List<GameSession> sessions, long totalElements, int page, int size) {
        public SessionPage { sessions = List.copyOf(sessions); }
        public long totalPages() { return size == 0 ? 0 : (totalElements + size - 1) / size; }
    }
}
