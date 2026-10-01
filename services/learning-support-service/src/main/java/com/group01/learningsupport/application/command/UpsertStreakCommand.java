package com.group01.learningsupport.application.command;

import java.time.LocalDate;
import java.util.UUID;

public record UpsertStreakCommand(
        UUID userId,
        int currentDays,
        int longestDays,
        LocalDate lastQualifiedDate,
        String timezone
) {
}
