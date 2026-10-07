package com.ieltspath.learning.api.dto.response;

import com.ieltspath.learning.domain.vo.LearningSkill;
import com.ieltspath.learning.domain.vo.PracticeSubmission;

import java.util.List;
import java.util.UUID;

public record PracticeSubmissionResponse(UUID attemptId, int correct, int total, double percent, boolean passed,
                                         boolean countedAsEvidence, List<SubmissionResponse.SolvedAnswer> results,
                                         String transcript, List<ReviewCreated> reviewsCreated,
                                         List<SkillScore> skillScores) {
    public record ReviewCreated(UUID reviewId, UUID knowledgePointId, String stage) {}
    public record SkillScore(LearningSkill skill, int correct, int total, double percent, boolean passed) {}

    public static PracticeSubmissionResponse from(PracticeSubmission result) {
        return new PracticeSubmissionResponse(result.attemptId(), result.correct(), result.total(), result.percent(),
                result.passed(), result.countedAsEvidence(), result.results().stream().map(answer ->
                new SubmissionResponse.SolvedAnswer(answer.questionVersionId(), answer.correct(),
                        answer.correctAnswer(), answer.explanation())).toList(), result.transcript(),
                result.reviewsCreated().stream().map(review -> new ReviewCreated(review.reviewId(),
                        review.knowledgePointId(), review.stage())).toList(),
                result.skillScores().stream().map(score -> new SkillScore(score.skill(), score.correct(),
                        score.total(), score.percent(), score.passed())).toList());
    }
}
