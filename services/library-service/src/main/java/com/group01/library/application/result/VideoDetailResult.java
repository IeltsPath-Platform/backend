package com.group01.library.application.result;

import com.group01.library.domain.vo.PublicationStatus;
import com.group01.library.domain.vo.VideoLevel;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record VideoDetailResult(
        UUID id,
        String youtubeVideoId,
        String youtubeUrl,
        String title,
        String description,
        String thumbnailUrl,
        Integer durationSeconds,
        UUID topicId,
        VideoLevel level,
        String requiredFeatureKey,
        PublicationStatus status,
        UUID createdBy,
        Instant createdAt,
        Instant updatedAt,
        List<VideoSegmentResult> segments
) {}
