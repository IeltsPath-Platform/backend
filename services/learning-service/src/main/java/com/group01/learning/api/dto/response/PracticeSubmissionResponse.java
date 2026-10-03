package com.group01.learning.api.dto.response;

import com.group01.learning.domain.vo.PracticeSubmission;
import java.util.List;
import java.util.UUID;

public record PracticeSubmissionResponse(UUID attemptId, int correct, int total, double percent, boolean passed,
                                         boolean countedAsEvidence, List<SubmissionResponse.SolvedAnswer> results,
                                         String transcript, List<ReviewCreated> reviewsCreated) {
    public record ReviewCreated(UUID reviewId, UUID knowledgePointId, String stage) {}

    public static PracticeSubmissionResponse from(PracticeSubmission result) {
        return new PracticeSubmissionResponse(result.attemptId(), result.correct(), result.total(), result.percent(),
                result.passed(), result.countedAsEvidence(), result.results().stream().map(answer ->
                new SubmissionResponse.SolvedAnswer(answer.questionVersionId(), answer.correct(),
                        answer.correctAnswer(), answer.explanation())).toList(), result.transcript(),
                result.reviewsCreated().stream().map(review -> new ReviewCreated(review.reviewId(),
                        review.knowledgePointId(), review.stage())).toList());
    }
}
