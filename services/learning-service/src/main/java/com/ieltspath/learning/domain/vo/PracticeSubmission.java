package com.ieltspath.learning.domain.vo;

import java.util.List;
import java.util.UUID;

/**
 * {@code passed} holds when every skill of the set passed; {@code skillScores} has one entry per skill in enum order
 * (absent from submissions stored before sets could mix skills).
 */
public record PracticeSubmission(UUID attemptId, int correct, int total, double percent, boolean passed,
                                 boolean countedAsEvidence, List<Answer> results, String transcript,
                                 List<ReviewCreated> reviewsCreated, List<SkillScore> skillScores) {
    public PracticeSubmission {
        skillScores = skillScores == null ? List.of() : List.copyOf(skillScores);
    }
    public PracticeSubmission(UUID attemptId, int correct, int total, double percent, boolean passed,
                              boolean countedAsEvidence, List<Answer> results, String transcript,
                              List<ReviewCreated> reviewsCreated) {
        this(attemptId, correct, total, percent, passed, countedAsEvidence, results, transcript, reviewsCreated,
                null);
    }
    public record SkillScore(LearningSkill skill, int correct, int total, double percent, boolean passed) {}
    public record Answer(UUID questionVersionId, boolean correct, String correctAnswer, String explanation) {}
    public record ReviewCreated(UUID reviewId, UUID knowledgePointId, String stage) {}
}
