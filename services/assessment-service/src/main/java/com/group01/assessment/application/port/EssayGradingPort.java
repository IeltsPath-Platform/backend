package com.group01.assessment.application.port;

import java.math.BigDecimal;

/** Grades one essay of a test with an LLM; implementations never log the essay, the prompt or credentials. */
public interface EssayGradingPort {
    /** False when no LLM is configured; jobs then go straight to a human examiner. */
    boolean available();

    /**
     * The overall IELTS band of {@code essay}.
     *
     * @throws com.group01.assessment.application.exception.EssayGradingException when the LLM is unavailable or its
     *                                                                          reply cannot be used
     */
    BigDecimal grade(Prompt prompt, String essay);

    /** {@code task} is {@code TASK_1} or {@code TASK_2}; {@code chartFacts} is required for Task 1. */
    record Prompt(String stem, String task, Integer minWords, String chartFacts) {}
}
