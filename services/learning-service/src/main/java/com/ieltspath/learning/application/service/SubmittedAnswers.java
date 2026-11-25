package com.ieltspath.learning.application.service;

import com.ieltspath.learning.application.command.SubmitExerciseCommand;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * What the learner wrote for each question of a submission, by question version, so the set can be shown again as it was filled
 * in. An unanswered question has no entry; a long answer is cut, because the grade is stored with them.
 */
public final class SubmittedAnswers {
    static final int MAX_LENGTH = 500;

    private SubmittedAnswers() {}

    public static Map<UUID, String> of(List<SubmitExerciseCommand.Answer> answers) {
        Map<UUID, String> submitted = new HashMap<>();
        if (answers == null) return submitted;
        for (SubmitExerciseCommand.Answer answer : answers) {
            if (!(answer.answer() instanceof String text) || text.isBlank()) continue;
            String trimmed = text.strip();
            submitted.put(answer.questionVersionId(),
                    trimmed.length() <= MAX_LENGTH ? trimmed : trimmed.substring(0, MAX_LENGTH));
        }
        return submitted;
    }
}
