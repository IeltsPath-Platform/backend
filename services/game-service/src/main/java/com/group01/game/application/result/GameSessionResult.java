package com.group01.game.application.result;

import com.group01.game.domain.aggregate.GameSession;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record GameSessionResult(UUID id, String gameType, String learningDomain, String mode, UUID topicId,
                                String status, int score, int itemCount, int answeredCount,
                                Instant startedAt, Instant endedAt, List<Map<String, Object>> items) {
    public static GameSessionResult from(GameSession session, boolean includeAnswers) {
        Object rawItems = session.sourceSnapshot().getOrDefault("items", List.of());
        List<Map<String, Object>> items = ((List<?>) rawItems).stream()
                .map(value -> (Map<String, Object>) value)
                .map(item -> {
                    if (includeAnswers) return java.util.Collections.unmodifiableMap(new java.util.LinkedHashMap<>(item));
                    var publicItem = new java.util.LinkedHashMap<>(item);
                    publicItem.remove("answerSpecJson");
                    publicItem.remove("explanation");
                    return java.util.Collections.unmodifiableMap(publicItem);
                }).toList();
        return new GameSessionResult(session.id(), session.gameType(), session.learningDomain(), session.mode(),
                session.topicId(), session.status().name(), session.score(), session.itemCount(),
                session.answeredCount(), session.startedAt(), session.endedAt(), items);
    }
}
