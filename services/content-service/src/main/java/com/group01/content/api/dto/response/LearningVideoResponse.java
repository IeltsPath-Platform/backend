package com.group01.content.api.dto.response;

import com.group01.content.application.result.LearningVideoResult;
import com.group01.content.domain.vo.AccessLevel;
import com.group01.content.domain.vo.PublicationStatus;
import com.group01.content.domain.vo.VideoLevel;

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
                result.accessLevel(),
                result.status(),
                result.createdBy(),
                result.createdAt(),
                result.updatedAt()
        );
    }
}

