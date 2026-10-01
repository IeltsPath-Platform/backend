package com.group01.learning.domain.service;

import java.util.Map;

public final class LessonHintPolicy {
    private final AnswerSpecGrader grader = new AnswerSpecGrader();

    public boolean eligible(Map<String, Object> answerSpec, int optionCount) {
        if (!grader.grade(answerSpec, null).gradable()) return false;
        Object type = answerSpec.getOrDefault("type", "CHOICE");
        return "FILL".equals(type) || "CHOICE".equals(type) && (optionCount == 0 || optionCount >= 3);
    }
}
