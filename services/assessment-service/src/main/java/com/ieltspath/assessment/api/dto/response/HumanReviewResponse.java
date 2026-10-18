package com.ieltspath.assessment.api.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.ieltspath.assessment.application.port.HumanReviewStore;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record HumanReviewResponse(UUID requestId, UUID reviewSubmissionId, String status, BigDecimal band,
                                 JsonNode criteria, List<String> errorCodes, String feedback) {
    public static HumanReviewResponse created(HumanReviewStore.Review review) {
        return new HumanReviewResponse(review.requestId(), review.reviewSubmissionId(), review.status(), null,
                null, null, null);
    }

    public static HumanReviewResponse result(HumanReviewStore.ReviewResult result) {
        return new HumanReviewResponse(null, result.reviewSubmissionId(), result.status(), result.band(),
                result.criteria(), result.errorCodes(), result.feedback());
    }
}
