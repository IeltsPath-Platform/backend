package com.ieltspath.learning.api.exception;

import com.ieltspath.learning.application.exception.LearningRequestException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LearningAiBusyResponseTest {
    @Test
    void bulkheadFullReturns503AiBusyWithRetryAfter() {
        var response = new GlobalExceptionHandler().learningRequest(
                new LearningRequestException(503, "AI_BUSY", "busy"));
        assertEquals(503, response.getStatusCode().value());
        assertEquals("5", response.getHeaders().getFirst("Retry-After"));
        assertEquals("AI_BUSY", response.getBody().code());
    }
}
