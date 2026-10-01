package com.group01.learningsupport.application.result;

import com.group01.learningsupport.domain.aggregate.VideoLearningProgress;
import com.group01.learningsupport.domain.vo.VideoProgressStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record VideoLearningProgressResult(UUID id, UUID userId, UUID videoId, int lastPositionMs,
                                          int watchedDurationSeconds, BigDecimal progressPercent,
                                          VideoProgressStatus status, Instant startedAt, Instant lastWatchedAt,
                                          Instant completedAt, Instant updatedAt) {
    public static VideoLearningProgressResult from(VideoLearningProgress progress) {
        return new VideoLearningProgressResult(progress.getId(), progress.getUserId(), progress.getVideoId(),
                progress.getLastPositionMs(), progress.getWatchedDurationSeconds(), progress.getProgressPercent(),
                progress.getStatus(), progress.getStartedAt(), progress.getLastWatchedAt(), progress.getCompletedAt(),
                progress.getUpdatedAt());
    }
}
