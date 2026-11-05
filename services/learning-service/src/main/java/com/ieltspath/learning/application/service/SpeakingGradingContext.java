package com.ieltspath.learning.application.service;

import com.ieltspath.learning.application.port.LearningContentClient.KnowledgePoint;

import java.util.List;

/**
 * What the grading job needs from Content besides the rubric: the Speaking error types the grader may name and the
 * knowledge points it may refer to. The job has no learner token and cannot call Content, so the learner's own request
 * captures this when the submission is completed (see {@link SpeakingGradingContextCapture}).
 */
public record SpeakingGradingContext(List<SpeakingGradingInput.ErrorOption> errorTypes,
                                     List<KnowledgePoint> knowledgePoints) {
    public SpeakingGradingContext {
        errorTypes = errorTypes == null ? List.of() : List.copyOf(errorTypes);
        knowledgePoints = knowledgePoints == null ? List.of() : List.copyOf(knowledgePoints);
    }

    public static SpeakingGradingContext empty() {
        return new SpeakingGradingContext(List.of(), List.of());
    }
}
