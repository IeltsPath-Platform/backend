package com.ieltspath.assessment.domain.entity;

import com.ieltspath.assessment.domain.vo.PracticeStatus;
import com.ieltspath.assessment.domain.vo.PracticeType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record VideoPracticeAttempt(UUID id, UUID userId, UUID videoId, UUID segmentId,
                                   PracticeType practiceType, String referenceTextSnapshot,
                                   String responseText, String audioReference, BigDecimal score,
                                   PracticeStatus status, Instant startedAt, Instant completedAt,
                                   Instant createdAt, String resultPayload) {
}
