package com.ieltspath.library.domain.aggregate;

import com.ieltspath.library.domain.DomainChecks;
import com.ieltspath.library.domain.exception.InvalidDataException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * One time a learner typed a video segment back in dictation practice. The frontend compares the text and decides
 * {@code correct}; the backend only validates and stores the result.
 */
@Getter
@Builder
@AllArgsConstructor
public class VideoDictationAttempt {
    public static final int TYPED_TEXT_MAX = 2_000;

    private final UUID id;
    private final UUID userId;
    private final UUID videoId;
    private final UUID segmentId;
    private final int attemptNo;
    private final String typedText;
    private final boolean correct;
    private final BigDecimal accuracyPercent;
    private final Instant createdAt;

    public static VideoDictationAttempt create(
            UUID userId,
            UUID videoId,
            UUID segmentId,
            int attemptNo,
            String typedText,
            boolean correct,
            BigDecimal accuracyPercent
    ) {
        if (videoId == null || segmentId == null || attemptNo <= 0) {
            throw new InvalidDataException("dictation attempt không hợp lệ");
        }
        if (accuracyPercent == null || accuracyPercent.compareTo(BigDecimal.ZERO) < 0
                || accuracyPercent.compareTo(new BigDecimal("100")) > 0) {
            throw new InvalidDataException("accuracyPercent không hợp lệ");
        }
        return new VideoDictationAttempt(
                UUID.randomUUID(),
                DomainChecks.userId(userId),
                videoId,
                segmentId,
                attemptNo,
                DomainChecks.required(typedText, TYPED_TEXT_MAX, "typedText"),
                correct,
                accuracyPercent,
                null
        );
    }
}
