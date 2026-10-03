package com.group01.learning.domain.vo;

import java.util.List;
import java.util.UUID;

public record PracticeSubmission(UUID attemptId, int correct, int total, double percent, boolean passed,
                                 boolean countedAsEvidence, List<Answer> results, String transcript,
                                 List<ReviewCreated> reviewsCreated) {
    public record Answer(UUID questionVersionId, boolean correct, String correctAnswer, String explanation) {}
    public record ReviewCreated(UUID reviewId, UUID knowledgePointId, String stage) {}
}
