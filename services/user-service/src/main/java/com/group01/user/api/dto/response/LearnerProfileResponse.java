package com.group01.user.api.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record LearnerProfileResponse(
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

