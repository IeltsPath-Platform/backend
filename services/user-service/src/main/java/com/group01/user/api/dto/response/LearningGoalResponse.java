package com.group01.user.api.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record LearningGoalResponse(
        UUID id,
        UUID userId,
        BigDecimal targetBand,
        LocalDate examDate,
        Integer availableMinutesPerDay,
        String status,
        LocalDateTime startedAt,
        LocalDateTime endedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}

