package com.ieltspath.assessment.application.port;

import java.math.BigDecimal;

/** Grades one essay of a test with an LLM; implementations never log the essay, the prompt or credentials. */
public interface EssayGradingPort {
    String PROMPT_VERSION = "essay-rubric-v1";
    /** False when no LLM is configured; jobs then go straight to a human examiner. */
    boolean available();

    /**
     * The overall IELTS band of {@code essay} and the examiner's comments on it.
     *
     * @throws com.ieltspath.assessment.application.exception.EssayGradingException when the LLM is unavailable or its
     *                                                                          reply cannot be used
     */
    EssayGrade grade(Prompt prompt, String essay);

    /** {@code task} is {@code TASK_1} or {@code TASK_2}; {@code chartFacts} is required for Task 1. */
    record Prompt(String stem, String task, Integer minWords, String chartFacts, String expectedPromptVersion) {
        public Prompt(String stem, String task, Integer minWords, String chartFacts) {
            this(stem, task, minWords, chartFacts, null);
        }
    }

    /**
     * {@code feedback} is JSON {@code {summary, criteria: [{code, band, comment}], focus: [..]}}, or null when the
     * reply carried bands without usable comments; the band alone is still a valid grade.
     */
    record EssayGrade(BigDecimal band, String feedback, String promptVersion, java.util.List<String> errorCodes) {
        public EssayGrade(BigDecimal band, String feedback) { this(band, feedback, PROMPT_VERSION, java.util.List.of()); }
        public EssayGrade(BigDecimal band, String feedback, String promptVersion) { this(band, feedback, promptVersion, java.util.List.of()); }
    }
}
