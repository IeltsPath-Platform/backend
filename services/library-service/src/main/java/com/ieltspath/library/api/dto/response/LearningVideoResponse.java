package com.ieltspath.library.api.dto.response;

import com.ieltspath.library.api.AccessLevelCompatibility;
import com.ieltspath.library.api.dto.AccessLevel;
import com.ieltspath.library.application.result.LearningVideoResult;
import com.ieltspath.library.domain.vo.PublicationStatus;
import com.ieltspath.library.domain.vo.VideoLevel;

import java.time.Instant;
import java.util.UUID;

public record LearningVideoResponse(
        UUID id,
        String youtubeVideoId,
        String youtubeUrl,
        String title,
        String description,
        String thumbnailUrl,
        Integer durationSeconds,
        UUID topicId,
        VideoLevel level,
        AccessLevel accessLevel,
        PublicationStatus status,
        UUID createdBy,
        Instant createdAt,
        Instant updatedAt
) {
    public static LearningVideoResponse from(LearningVideoResult result) {
        return new LearningVideoResponse(
                result.id(),
                result.youtubeVideoId(),
                result.youtubeUrl(),
                result.title(),
                result.description(),
                result.thumbnailUrl(),
                result.durationSeconds(),
                result.topicId(),
                result.level(),
                AccessLevelCompatibility.toAccessLevel(result.requiredFeatureKey()),
                result.status(),
                result.createdBy(),
                result.createdAt(),
                result.updatedAt()
        );
    }
}
