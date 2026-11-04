package com.ieltspath.learning.api.dto.response;

import com.fasterxml.jackson.databind.JsonNode;
import com.ieltspath.learning.application.port.HumanReviewClient;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * The examiner's review as the learner reads it: the review fields as Assessment returns them, plus the study points the review
 * produced once it was applied to mastery ({@code []} before that).
 */
public record HumanReviewResponse(UUID reviewSubmissionId, String status, BigDecimal band,
                                  List<HumanReviewClient.Criterion> criteria, List<String> errorCodes, String feedback,
                                  List<HumanReviewClient.Annotation> annotations, UUID examinerId,
                                  List<JsonNode> studyPoints) {
    public static HumanReviewResponse of(HumanReviewClient.Review review, List<JsonNode> studyPoints) {
        return new HumanReviewResponse(review.reviewSubmissionId(), review.status(), review.band(), review.criteria(),
                review.errorCodes(), review.feedback(), review.annotations(), review.examinerId(), studyPoints);
    }
}
