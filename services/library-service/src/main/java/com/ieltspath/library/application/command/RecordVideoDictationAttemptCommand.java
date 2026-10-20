package com.ieltspath.library.application.command;

import java.math.BigDecimal;
import java.util.UUID;

public record RecordVideoDictationAttemptCommand(
        UUID userId,
        UUID segmentId,
        String typedText,
        boolean correct,
        BigDecimal accuracyPercent
) {
}
