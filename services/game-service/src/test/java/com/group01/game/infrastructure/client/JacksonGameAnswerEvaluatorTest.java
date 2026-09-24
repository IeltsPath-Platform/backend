package com.group01.game.infrastructure.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JacksonGameAnswerEvaluatorTest {
    private final JacksonGameAnswerEvaluator evaluator = new JacksonGameAnswerEvaluator(new ObjectMapper());

    @Test
    void trimsAndIgnoresCaseForTextAnswers() {
        assertTrue(evaluator.isCorrect("{\"answer\":\"  London \"}", "london"));
    }

    @Test
    void comparesNumbersAndObjectsStructurally() {
        assertTrue(evaluator.isCorrect("42", 42));
        assertTrue(evaluator.isCorrect("{\"a\":1,\"b\":true}", Map.of("b", true, "a", 1)));
    }

    @Test
    void acceptsAnyArrayCandidateAndUsesFallbackKeyOrder() {
        assertTrue(evaluator.isCorrect("[\"red\",\"blue\"]", "BLUE"));
        assertTrue(evaluator.isCorrect("{\"answer\":\"first\",\"expected\":\"second\"}", "first"));
    }

    @Test
    void rejectsNullValuesAndSurfacesCorruptStoredKeys() {
        assertFalse(evaluator.isCorrect("\"answer\"", null));
        assertFalse(evaluator.isCorrect(null, "answer"));
        assertThrows(IllegalStateException.class, () -> evaluator.isCorrect("{invalid", "answer"));
    }
}
