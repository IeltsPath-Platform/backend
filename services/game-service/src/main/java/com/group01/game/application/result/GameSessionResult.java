package com.group01.game.application.result;

import com.group01.game.domain.aggregate.GameSession;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record GameSessionResult(UUID id, String gameType, String learningDomain, String mode, UUID topicId,
                                String status, int score, int itemCount, int answeredCount,
                                Instant startedAt, Instant endedAt, List<GameItemResult> items) {
    public GameSessionResult {
        items = List.copyOf(items);
    }

    public static GameSessionResult from(GameSession session) {
        Object rawItems = session.sourceSnapshot().getOrDefault("items", List.of());
        List<GameItemResult> items = ((List<?>) rawItems).stream()
                .map(value -> (Map<String, Object>) value)
                .map(GameItemResult::from).toList();
        return new GameSessionResult(session.id(), session.gameType(), session.learningDomain(), session.mode(),
                session.topicId(), session.status().name(), session.score(), session.itemCount(),
                session.answeredCount(), session.startedAt(), session.endedAt(), items);
    }
}
