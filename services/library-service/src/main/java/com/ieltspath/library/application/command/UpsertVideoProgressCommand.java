package com.ieltspath.library.application.command;

import com.ieltspath.library.domain.vo.VideoProgressStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record UpsertVideoProgressCommand(
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
}
