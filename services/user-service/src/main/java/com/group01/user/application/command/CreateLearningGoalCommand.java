package com.group01.user.application.command;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CreateLearningGoalCommand(
        UUID userId,
        BigDecimal targetBand,
        LocalDate examDate,
        Integer availableMinutesPerDay
) {
}

