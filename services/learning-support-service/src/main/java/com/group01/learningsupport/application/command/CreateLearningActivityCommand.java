package com.group01.learningsupport.application.command;

import java.time.Instant;
import java.util.UUID;

public record CreateLearningActivityCommand(
        UUID userId,
        String activityType,
        String sourceType,
        UUID sourceId,
        Instant occurredAt,
        Integer durationSeconds
) {
}
