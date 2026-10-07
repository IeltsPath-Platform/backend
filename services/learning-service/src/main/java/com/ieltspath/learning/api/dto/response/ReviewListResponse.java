package com.ieltspath.learning.api.dto.response;

import com.ieltspath.learning.domain.vo.LearningSkill;
import com.ieltspath.learning.domain.vo.ReviewListEntry;

import java.time.Instant;
import java.util.UUID;

public record ReviewListResponse(UUID reviewId, UUID knowledgePointId, UUID lessonId,
                                 LearningSkill skill, String stage, Instant createdAt) {
    public static ReviewListResponse from(ReviewListEntry entry) {
        return new ReviewListResponse(entry.reviewId(), entry.knowledgePointId(), entry.lessonId(),
                entry.skill(), entry.stage().name(), entry.createdAt());
    }
}
