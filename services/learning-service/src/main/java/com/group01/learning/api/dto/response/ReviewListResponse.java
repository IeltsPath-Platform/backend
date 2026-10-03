package com.group01.learning.api.dto.response;

import com.group01.learning.domain.vo.LearningSkill;
import com.group01.learning.domain.vo.ReviewListEntry;

import java.time.Instant;
import java.util.UUID;

public record ReviewListResponse(UUID reviewId, UUID knowledgePointId, UUID lessonId,
                                 LearningSkill skill, String stage, Instant createdAt) {
    public static ReviewListResponse from(ReviewListEntry entry) {
        return new ReviewListResponse(entry.reviewId(), entry.knowledgePointId(), entry.lessonId(),
                entry.skill(), "PRACTICE", entry.createdAt());
    }
}
