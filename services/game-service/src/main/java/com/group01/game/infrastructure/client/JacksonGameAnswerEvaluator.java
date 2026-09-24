package com.group01.game.infrastructure.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.group01.game.application.port.GameAnswerEvaluator;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class JacksonGameAnswerEvaluator implements GameAnswerEvaluator {
    private static final List<String> ANSWER_KEYS =
            List.of("answer", "correctAnswer", "correctOptionId", "optionId", "expected");

    private final ObjectMapper objectMapper;

    public JacksonGameAnswerEvaluator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean isCorrect(Object answerSpec, Object submittedAnswer) {
        if (submittedAnswer == null || answerSpec == null) return false;
        try {
            JsonNode expected = objectMapper.readTree(answerSpec.toString());
            JsonNode actual = objectMapper.valueToTree(submittedAnswer);
            if (expected.isObject()) {
                for (String key : ANSWER_KEYS) {
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
}
