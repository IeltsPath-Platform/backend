package com.ieltspath.learning.domain.service;

import java.util.List;
import java.util.Map;

/** When a question's hint may be shown: only after a wrong answer, and only for question types a hint helps with. */
public final class LessonHintPolicy {
    private final AnswerSpecGrader grader = new AnswerSpecGrader();

    public boolean eligible(Map<String, Object> answerSpec, int optionCount) {
        if (!grader.grade(answerSpec, null).gradable()) return false;
        Object type = answerSpec.getOrDefault("type", "CHOICE");
        return "FILL".equals(type) || "CHOICE".equals(type) && (optionCount == 0 || optionCount >= 3);
    }

    /** The hint to reveal for an answered question, or null: nothing for a right answer, and nothing before answering. */
    public String afterAnswer(boolean correct, Map<String, Object> answerSpec, List<?> options, String hint) {
        if (correct || hint == null || hint.isBlank()) return null;
        return eligible(answerSpec, options == null ? 0 : options.size()) ? hint : null;
    }
}
