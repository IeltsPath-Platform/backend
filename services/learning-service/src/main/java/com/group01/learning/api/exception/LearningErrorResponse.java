package com.group01.learning.api.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record LearningErrorResponse(String detail, String code, List<Review> reviews) {
    public record Review(UUID reviewId, UUID lessonId, UUID knowledgePointId) {}
}
