package com.group01.learningsupport.api.dto.response;

import com.group01.learningsupport.application.result.LearningActivityResult;

import java.time.Instant;
import java.util.UUID;

public record LearningActivityResponse(UUID id, UUID userId, String activityType, String sourceType, UUID sourceId,
                                       Instant occurredAt, Integer durationSeconds, Instant verifiedAt) {
    public static LearningActivityResponse from(LearningActivityResult result) {
        return new LearningActivityResponse(result.id(), result.userId(), result.activityType(), result.sourceType(),
                result.sourceId(), result.occurredAt(), result.durationSeconds(), result.verifiedAt());
    }
}
