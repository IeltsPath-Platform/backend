package com.ieltspath.learning.domain.vo;

import java.util.List;
import java.util.UUID;

/**
 * {@code passed} holds when every skill of the set passed; {@code skillScores} has one entry per skill in enum order
 * (absent from submissions stored before sets could mix skills).
 */
public record PracticeSubmission(UUID attemptId, int correct, int total, double percent, boolean passed,
                                 boolean countedAsEvidence, List<Answer> results, String transcript,
                                 List<ReviewCreated> reviewsCreated, List<SkillScore> skillScores,
                                 GamificationSummary gamification) {
    public PracticeSubmission {
        skillScores = skillScores == null ? List.of() : List.copyOf(skillScores);
    }
    public PracticeSubmission(UUID attemptId, int correct, int total, double percent, boolean passed,
                              boolean countedAsEvidence, List<Answer> results, String transcript,
                              List<ReviewCreated> reviewsCreated, List<SkillScore> skillScores) {
        this(attemptId, correct, total, percent, passed, countedAsEvidence, results, transcript, reviewsCreated,
                skillScores, null);
    }
    public PracticeSubmission(UUID attemptId, int correct, int total, double percent, boolean passed,
                              boolean countedAsEvidence, List<Answer> results, String transcript,
                              List<ReviewCreated> reviewsCreated) {
        this(attemptId, correct, total, percent, passed, countedAsEvidence, results, transcript, reviewsCreated,
                null, null);
    }
    public record SkillScore(LearningSkill skill, int correct, int total, double percent, boolean passed) {}
    /** {@code hint} is set only for a wrong objective answer whose question type a hint helps with. */
    public record Answer(UUID questionVersionId, boolean correct, String correctAnswer, String explanation, String hint,
                         ErrorTrap error) {
        public Answer(UUID questionVersionId, boolean correct, String correctAnswer, String explanation, String hint) {
            this(questionVersionId, correct, correctAnswer, explanation, hint, null);
        }
        public Answer(UUID questionVersionId, boolean correct, String correctAnswer, String explanation) {
            this(questionVersionId, correct, correctAnswer, explanation, null, null);
        }
    }
    public record ErrorTrap(String code, String name, String fixHint) {}
    public record ReviewCreated(UUID reviewId, UUID knowledgePointId, String stage, boolean blocking) {
        public ReviewCreated(UUID reviewId, UUID knowledgePointId, String stage) {
            this(reviewId, knowledgePointId, stage, true);
        }
    }
}
