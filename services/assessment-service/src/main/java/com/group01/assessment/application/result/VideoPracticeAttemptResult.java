package com.group01.assessment.application.result;

import com.group01.assessment.domain.vo.PracticeStatus;
import com.group01.assessment.domain.vo.PracticeType;

import java.time.Instant;
import java.util.UUID;

public record VideoPracticeAttemptResult(UUID id, UUID userId, UUID videoId, PracticeType practiceType,
                                         PracticeStatus status, Instant startedAt, Instant completedAt,
                                         String resultPayload) {
}
