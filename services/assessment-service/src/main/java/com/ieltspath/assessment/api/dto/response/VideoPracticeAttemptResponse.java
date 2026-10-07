package com.ieltspath.assessment.api.dto.response;

import com.ieltspath.assessment.application.result.VideoPracticeAttemptResult;
import com.ieltspath.assessment.domain.vo.PracticeStatus;
import com.ieltspath.assessment.domain.vo.PracticeType;

import java.time.Instant;
import java.util.UUID;

public record VideoPracticeAttemptResponse(UUID id, UUID userId, UUID videoId, PracticeType practiceType,
                                           PracticeStatus status, Instant startedAt, Instant completedAt,
                                           String resultSnapshot) {
    public static VideoPracticeAttemptResponse from(VideoPracticeAttemptResult r) {
        return new VideoPracticeAttemptResponse(r.id(), r.userId(), r.videoId(), r.practiceType(), r.status(),
                r.startedAt(), r.completedAt(), r.resultPayload());
    }
}
