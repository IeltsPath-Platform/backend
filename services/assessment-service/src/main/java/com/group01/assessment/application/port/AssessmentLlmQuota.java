package com.group01.assessment.application.port;

import java.time.LocalDate;
import java.util.UUID;

/** LLM grading calls per learner and day. */
public interface AssessmentLlmQuota {
    /** Counts one call and returns true, or returns false without counting when {@code limit} is already reached. */
    boolean tryConsume(UUID userId, LocalDate day, int limit);
}
