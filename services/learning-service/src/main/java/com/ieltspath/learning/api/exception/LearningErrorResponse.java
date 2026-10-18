package com.ieltspath.learning.api.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import com.ieltspath.learning.domain.vo.LearningSkill;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record LearningErrorResponse(String detail, String code, List<Review> reviews, UUID submissionId,
                                    List<UUID> lessonIds, Instant nextHeartAt, Integer refillCost) {
    public LearningErrorResponse(String detail, String code, List<Review> reviews) {
        this(detail, code, reviews, null, null, null, null);
    }

    public LearningErrorResponse(String detail, String code, List<Review> reviews, UUID submissionId) {
        this(detail, code, reviews, submissionId, null, null, null);
    }

    public LearningErrorResponse(String detail, String code, List<Review> reviews, UUID submissionId,
                                 List<UUID> lessonIds) {
        this(detail, code, reviews, submissionId, lessonIds, null, null);
    }

    public record Review(UUID reviewId, UUID lessonId, UUID knowledgePointId,
                         @JsonInclude(JsonInclude.Include.NON_NULL) LearningSkill skill) {}
}
