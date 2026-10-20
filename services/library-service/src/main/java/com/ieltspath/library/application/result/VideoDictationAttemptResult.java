package com.ieltspath.library.application.result;

import com.ieltspath.library.domain.aggregate.VideoDictationAttempt;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record VideoDictationAttemptResult(UUID id, UUID videoId, UUID segmentId, int attemptNo, String typedText,
                                          boolean correct, BigDecimal accuracyPercent, Instant createdAt) {
    public static VideoDictationAttemptResult from(VideoDictationAttempt attempt) {
        return new VideoDictationAttemptResult(attempt.getId(), attempt.getVideoId(), attempt.getSegmentId(),
                attempt.getAttemptNo(), attempt.getTypedText(), attempt.isCorrect(), attempt.getAccuracyPercent(),
                attempt.getCreatedAt());
    }
}
