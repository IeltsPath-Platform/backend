package com.group01.user.application.result;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record LearnerProfileResult(
        UUID userId,
        String displayName,
        String avatarReference,
        String bio,
        BigDecimal selfReportedBand,
        String timezone,
        String visibility,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}

