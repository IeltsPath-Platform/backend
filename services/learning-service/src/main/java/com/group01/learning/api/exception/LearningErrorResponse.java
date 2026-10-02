package com.group01.learning.api.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record LearningErrorResponse(String detail, String code, List<Review> reviews, UUID submissionId) {
    public LearningErrorResponse(String detail, String code, List<Review> reviews) {
        this(detail, code, reviews, null);
    }

    public record Review(UUID reviewId, UUID lessonId, UUID knowledgePointId) {}
}
