package com.group01.learningsupport.domain.aggregate;

import com.group01.learningsupport.domain.exception.InvalidDataException;

import com.group01.learningsupport.domain.DomainChecks;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class LearningActivity {
    private final UUID id;
    private final UUID userId;
    private final String activityType;
    private final String sourceType;
    private final UUID sourceId;
    private final Instant occurredAt;
    private final Integer durationSeconds;
    private final Instant verifiedAt;

    public static LearningActivity create(
            UUID userId,
            String activityType,
            String sourceType,
            UUID sourceId,
            Instant occurredAt,
            Integer durationSeconds
    ) {
        if (sourceId == null || occurredAt == null) {
            throw new InvalidDataException("activity không hợp lệ");
        }
        if (durationSeconds != null && durationSeconds < 0) {
            throw new InvalidDataException("durationSeconds không hợp lệ");
        }
        return new LearningActivity(
                UUID.randomUUID(),
                DomainChecks.userId(userId),
                DomainChecks.required(activityType, 50, "activityType"),
                DomainChecks.required(sourceType, 50, "sourceType"),
                sourceId,
                occurredAt,
                durationSeconds,
                null
        );
    }
}
