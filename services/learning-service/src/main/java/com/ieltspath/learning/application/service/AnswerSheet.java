package com.ieltspath.learning.application.service;

import com.ieltspath.learning.application.command.SubmitExerciseCommand;
import com.ieltspath.learning.application.exception.LearningRequestException;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Rules shared by lesson blocks and review sets: one answer per question, (the pass mark is {@link com.ieltspath.learning.domain.service.PassMark}). */
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


    private static LearningRequestException invalid() {
        return new LearningRequestException(422, "INVALID_ANSWERS", "Submit one answer for every exercise question");
    }
}
