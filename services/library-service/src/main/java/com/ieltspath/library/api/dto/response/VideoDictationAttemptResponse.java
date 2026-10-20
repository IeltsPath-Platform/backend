package com.ieltspath.library.api.dto.response;

import com.ieltspath.library.application.result.VideoDictationAttemptResult;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record VideoDictationAttemptResponse(UUID id, UUID videoId, UUID segmentId, int attemptNo, String typedText,
                                            boolean correct, BigDecimal accuracyPercent, Instant createdAt) {
    public static VideoDictationAttemptResponse from(VideoDictationAttemptResult result) {
        return new VideoDictationAttemptResponse(result.id(), result.videoId(), result.segmentId(), result.attemptNo(),
                result.typedText(), result.correct(), result.accuracyPercent(), result.createdAt());
    }
}
