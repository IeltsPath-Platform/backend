package com.ieltspath.learning.application.service;

import com.ieltspath.learning.application.command.SubmitExerciseCommand;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Time per question as the client reported it. Questions the client did not time are absent from the map. */
public final class AnswerDurations {
    private AnswerDurations() {}

    public static Map<UUID, Long> of(List<SubmitExerciseCommand.Answer> answers) {
        Map<UUID, Long> durations = new HashMap<>();
        if (answers == null) return durations;
        for (SubmitExerciseCommand.Answer answer : answers) {
            if (answer.durationMs() != null) durations.put(answer.questionVersionId(), answer.durationMs());
        }
        return durations;
    }
}
