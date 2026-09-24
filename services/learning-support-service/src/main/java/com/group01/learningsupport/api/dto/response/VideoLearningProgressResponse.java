package com.group01.learningsupport.api.dto.response;

import com.group01.learningsupport.application.result.VideoLearningProgressResult;
import com.group01.learningsupport.domain.vo.VideoProgressStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record VideoLearningProgressResponse(UUID id, UUID userId, UUID videoId, int lastPositionMs,
                                            int watchedDurationSeconds, BigDecimal progressPercent,
                                            VideoProgressStatus status, Instant startedAt, Instant lastWatchedAt,
                                            Instant completedAt, Instant updatedAt) {
    public static VideoLearningProgressResponse from(VideoLearningProgressResult result) {
        return new VideoLearningProgressResponse(result.id(), result.userId(), result.videoId(),
                result.lastPositionMs(), result.watchedDurationSeconds(), result.progressPercent(), result.status(),
                result.startedAt(), result.lastWatchedAt(), result.completedAt(), result.updatedAt());
    }
}
