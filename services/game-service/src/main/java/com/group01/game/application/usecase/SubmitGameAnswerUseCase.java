package com.group01.game.application.usecase;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.group01.game.application.port.OutboxWriter;
import com.group01.game.application.port.GameMatchEventWriter;
import com.group01.game.application.result.GameAnswerResult;
import com.group01.game.domain.aggregate.GameAnswer;
import com.group01.game.domain.aggregate.GameSession;
import com.group01.game.domain.exception.GameSessionNotFoundException;
import com.group01.game.domain.repository.GameSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
public class SubmitGameAnswerUseCase {
    private final GameSessionRepository sessionRepository;
    private final ObjectMapper objectMapper;
    private final OutboxWriter outboxWriter;
    private final GameMatchEventWriter matchEventWriter;

    public SubmitGameAnswerUseCase(GameSessionRepository sessionRepository, ObjectMapper objectMapper,
                                   OutboxWriter outboxWriter, GameMatchEventWriter matchEventWriter) {
        this.sessionRepository = sessionRepository;
        this.objectMapper = objectMapper;
        this.outboxWriter = outboxWriter;
        this.matchEventWriter = matchEventWriter;
    }

    @Transactional
    public GameAnswerResult execute(UUID sessionId, UUID userId, int itemSequence,
                                    Map<String, Object> responsePayload, long durationMilliseconds) {
        GameSession session = sessionRepository.findOwned(sessionId, userId)
                .orElseThrow(() -> new GameSessionNotFoundException(sessionId));
        var existing = sessionRepository.findAnswer(sessionId, itemSequence);
        if (existing.isPresent()) {
            GameAnswer answer = existing.get();
            if (Objects.equals(answer.responsePayload(), responsePayload)) {
                return new GameAnswerResult(answer.id(), itemSequence, answer.correct(), session.score(),
                        session.status().name(), true);
            }
            throw new IllegalStateException("This item has already been answered with a different response");
        }
        if (itemSequence < 1 || itemSequence > session.itemCount()) {
            throw new IllegalArgumentException("itemSequence is outside this session");
        }
        if (durationMilliseconds < 0) throw new IllegalArgumentException("durationMilliseconds must not be negative");
        Object rawItems = session.sourceSnapshot().get("items");
        Map<String, Object> item = (Map<String, Object>) ((List<?>) rawItems).get(itemSequence - 1);
        boolean correct = isCorrect(item.get("answerSpecJson"), responsePayload.get("answer"));
        session.recordAnswer(correct, Instant.now());
        GameAnswer answer = new GameAnswer(UUID.randomUUID(), sessionId, itemSequence,
                asUuid(item.get("vocabularySenseId")), asUuid(item.get("questionVersionId")),
                item, responsePayload, correct, durationMilliseconds);
        sessionRepository.saveAnswer(answer);
        GameSession saved = sessionRepository.save(session);
        boolean matchCompleted = session.matchPlayerId() != null && matchEventWriter.recordAnswer(answer, saved);
        outboxWriter.append("GameSession", sessionId.toString(), "GameAnswerSubmitted",
                Map.of("sessionId", sessionId.toString(), "itemSequence", itemSequence,
                        "isCorrect", correct, "score", saved.score()));
        if (matchCompleted) {
            outboxWriter.append("GameMatch", session.matchPlayerId().toString(), "GameMatchCompleted",
                    Map.of("matchPlayerId", session.matchPlayerId().toString()));
        }
        return new GameAnswerResult(answer.id(), itemSequence, correct, saved.score(), saved.status().name(), false);
    }

    private boolean isCorrect(Object answerSpecValue, Object submittedValue) {
        if (submittedValue == null || answerSpecValue == null) return false;
        try {
            JsonNode expected = objectMapper.readTree(answerSpecValue.toString());
            JsonNode actual = objectMapper.valueToTree(submittedValue);
            if (expected.isObject()) {
                for (String key : List.of("answer", "correctAnswer", "correctOptionId", "optionId", "expected")) {
                    if (expected.has(key)) return matches(expected.get(key), actual);
                }
            }
            if (expected.isArray()) {
                for (JsonNode candidate : expected) if (matches(candidate, actual)) return true;
                return false;
            }
            return matches(expected, actual);
        } catch (Exception exception) {
            throw new IllegalStateException("Stored game answer key is invalid", exception);
        }
    }

    private boolean matches(JsonNode expected, JsonNode actual) {
        if (expected.isTextual() && actual.isTextual()) {
            return expected.asText().trim().equalsIgnoreCase(actual.asText().trim());
        }
        return expected.equals(actual);
    }

    private UUID asUuid(Object value) { return value == null ? null : UUID.fromString(value.toString()); }
}
