package com.ieltspath.learning.application.result;

import com.ieltspath.learning.domain.vo.GamificationSummary;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.UUID;

/** {@code gamification} is what this submission did to the learner's XP; null in responses stored before XP existed. */
public record SubmissionResult(boolean blockPassed, boolean lessonCompleted, List<AnswerResult> results,
                               GamificationSummary gamification) {
    public SubmissionResult(boolean blockPassed, boolean lessonCompleted, List<AnswerResult> results) {
        this(blockPassed, lessonCompleted, results, null);
    }

    public record AnswerResult(UUID questionVersionId, boolean correct, String correctAnswer, String explanation,
                               String hint, @JsonInclude(JsonInclude.Include.NON_NULL) ErrorTrapResult error) {
        public AnswerResult(UUID questionVersionId, boolean correct, String correctAnswer, String explanation,
                            String hint) {
            this(questionVersionId, correct, correctAnswer, explanation, hint, null);
        }
        public AnswerResult(UUID questionVersionId, boolean correct, String correctAnswer, String explanation) {
            this(questionVersionId, correct, correctAnswer, explanation, null, null);
        }
    }
}
