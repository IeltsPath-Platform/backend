package com.group01.learning.application.result;

import java.util.List;
import java.util.UUID;

public record SubmissionResult(boolean blockPassed, boolean lessonCompleted, List<AnswerResult> results) {
    public record AnswerResult(UUID questionVersionId, boolean correct, String correctAnswer, String explanation,
                               String hint) {
        public AnswerResult(UUID questionVersionId, boolean correct, String correctAnswer, String explanation) {
            this(questionVersionId, correct, correctAnswer, explanation, null);
        }
    }
}
