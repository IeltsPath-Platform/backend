package com.group01.learning.api.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import java.util.UUID;
import com.group01.learning.domain.vo.LearningSkill;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record LearningErrorResponse(String detail, String code, List<Review> reviews, UUID submissionId,
                                    List<UUID> lessonIds) {
    public LearningErrorResponse(String detail, String code, List<Review> reviews) {
        this(detail, code, reviews, null, null);
    }

    public LearningErrorResponse(String detail, String code, List<Review> reviews, UUID submissionId) {
        this(detail, code, reviews, submissionId, null);
    }

    public record Review(UUID reviewId, UUID lessonId, UUID knowledgePointId,
                         @JsonInclude(JsonInclude.Include.NON_NULL) LearningSkill skill) {}
}
