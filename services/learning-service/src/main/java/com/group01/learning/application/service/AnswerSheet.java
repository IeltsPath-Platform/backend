package com.group01.learning.application;

import com.group01.learning.application.command.SubmitExerciseCommand;
import com.group01.learning.application.exception.LearningRequestException;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Rules shared by lesson blocks and review sets: one answer per question, and the 70% pass mark. */
public final class AnswerSheet {
    private AnswerSheet() {}

    /** Answers keyed by question; every question must be answered exactly once and nothing else may be sent. */
    public static Map<UUID, Object> require(List<UUID> questionIds, List<SubmitExerciseCommand.Answer> answers) {
        Map<UUID, Object> byQuestion = new HashMap<>();
        Set<UUID> ids = new HashSet<>();
        for (var answer : answers) {
            if (!ids.add(answer.questionVersionId())) throw invalid();
            byQuestion.put(answer.questionVersionId(), answer.answer());
        }
        if (questionIds.isEmpty() || !ids.equals(Set.copyOf(questionIds))) throw invalid();
        return byQuestion;
    }

    public static boolean passes(long correct, int total) {
        return correct * 10L >= total * 7L;
    }

    private static LearningRequestException invalid() {
        return new LearningRequestException(422, "INVALID_ANSWERS", "Submit one answer for every exercise question");
    }
}
