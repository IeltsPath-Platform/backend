package com.group01.learningsupport.domain.aggregate;

import com.group01.learningsupport.domain.exception.InvalidDataException;

import com.group01.learningsupport.domain.DomainChecks;
import com.group01.learningsupport.domain.vo.VideoProgressStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class VideoLearningProgress {
    private final UUID id;
    private final UUID userId;
    private final UUID videoId;
    private int lastPositionMs;
    private int watchedDurationSeconds;
    private BigDecimal progressPercent;
    private VideoProgressStatus status;
    private Instant startedAt;
    private Instant lastWatchedAt;
    private Instant completedAt;
    private Instant updatedAt;

    public static VideoLearningProgress create(
            UUID userId,
            UUID videoId,
            int lastPositionMs,
            int watchedDurationSeconds,
            BigDecimal progressPercent,
            VideoProgressStatus status,
            Instant startedAt,
            Instant lastWatchedAt,
            Instant completedAt
    ) {
        if (videoId == null || status == null) {
            throw new InvalidDataException("video progress không hợp lệ");
        }
        return new VideoLearningProgress(
                UUID.randomUUID(),
                DomainChecks.userId(userId),
                videoId,
                DomainChecks.nonNegative(lastPositionMs, "lastPositionMs"),
                DomainChecks.nonNegative(watchedDurationSeconds, "watchedDurationSeconds"),
                percent(progressPercent),
                status,
                startedAt,
                lastWatchedAt,
                completedAt,
                null
        );
    }

    public void replace(
            int lastPositionMs,
            int watchedDurationSeconds,
            BigDecimal progressPercent,
            VideoProgressStatus status,
            Instant startedAt,
            Instant lastWatchedAt,
            Instant completedAt
    ) {
        if (status == null) {
            throw new InvalidDataException("status không hợp lệ");
        }
        this.lastPositionMs = DomainChecks.nonNegative(lastPositionMs, "lastPositionMs");
        this.watchedDurationSeconds = DomainChecks.nonNegative(watchedDurationSeconds, "watchedDurationSeconds");
        this.progressPercent = percent(progressPercent);
        this.status = status;
        this.startedAt = startedAt;
        this.lastWatchedAt = lastWatchedAt;
        this.completedAt = completedAt;
    }

    private static BigDecimal percent(BigDecimal value) {
        if (value == null || value.compareTo(BigDecimal.ZERO) < 0 || value.compareTo(new BigDecimal("100")) > 0) {
            throw new InvalidDataException("progressPercent không hợp lệ");
        }
        return value;
    }
}
