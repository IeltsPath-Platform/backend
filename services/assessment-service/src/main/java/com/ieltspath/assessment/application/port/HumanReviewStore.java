package com.ieltspath.assessment.application.port;

import com.fasterxml.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HumanReviewStore {
    Review create(CreateReview review);
    Optional<ReviewResult> find(UUID reviewId, UUID learnerId);
    void complete(UUID jobId, UUID examinerId, BigDecimal band, JsonNode criteria, List<String> errorCodes,
                  String feedback);

    record CreateReview(UUID requestId, UUID learnerId, UUID sourceSubmissionId, String source, String task,
                        JsonNode promptSnapshot, String essay, JsonNode rubric) {}
    record Review(UUID requestId, UUID reviewSubmissionId, String status) {}
    record ReviewResult(UUID reviewSubmissionId, String status, BigDecimal band, JsonNode criteria,
                        List<String> errorCodes, String feedback) {}
}
